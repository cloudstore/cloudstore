package co.codewizards.cloudstore.ls.rest.server;

import static co.codewizards.cloudstore.core.chronos.ChronosUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;
import static java.util.Objects.*;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.core.dto.Error;
import co.codewizards.cloudstore.core.dto.RemoteException;
import co.codewizards.cloudstore.core.dto.RemoteExceptionUtil;
import co.codewizards.cloudstore.core.io.TimeoutException;
import co.codewizards.cloudstore.core.util.ExceptionUtil;
import co.codewizards.cloudstore.ls.core.dto.ErrorResponse;
import co.codewizards.cloudstore.ls.core.dto.InverseServiceRequest;
import co.codewizards.cloudstore.ls.core.dto.InverseServiceResponse;
import co.codewizards.cloudstore.ls.core.dto.NullResponse;
import co.codewizards.cloudstore.ls.core.invoke.ClassInfo;
import co.codewizards.cloudstore.ls.core.invoke.ClassInfoMap;
import co.codewizards.cloudstore.ls.core.invoke.ClassManager;
import co.codewizards.cloudstore.ls.core.invoke.DelayedMethodInvocationResponse;
import co.codewizards.cloudstore.ls.core.invoke.IncDecRefCountQueue;
import co.codewizards.cloudstore.ls.core.invoke.InverseMethodInvocationRequest;
import co.codewizards.cloudstore.ls.core.invoke.InverseMethodInvocationResponse;
import co.codewizards.cloudstore.ls.core.invoke.Invoker;
import co.codewizards.cloudstore.ls.core.invoke.MethodInvocationRequest;
import co.codewizards.cloudstore.ls.core.invoke.MethodInvocationResponse;
import co.codewizards.cloudstore.ls.core.invoke.ObjectManager;
import co.codewizards.cloudstore.ls.core.invoke.ObjectRef;
import co.codewizards.cloudstore.ls.core.invoke.RemoteObjectProxy;
import co.codewizards.cloudstore.ls.core.invoke.RemoteObjectProxyFactory;
import co.codewizards.cloudstore.ls.core.invoke.RemoteObjectProxyInvocationHandler;

public class InverseInvoker implements Invoker {
	/**
	 * Timeout (in milliseconds) before sending an empty HTTP response to the polling client. The client does
	 * <i>long polling</i> in order to allow for
	 * {@linkplain #performInverseServiceRequest(InverseServiceRequest) inverse service invocations}.
	 * <p>
	 * This timeout must be (significantly) shorter than {@link ObjectManager#EVICT_UNUSED_OBJECT_MANAGER_TIMEOUT_MS} to make sure, the
	 * {@linkplain #pollInverseServiceRequest() polling} serves additionally as a keep-alive for
	 * the server-side {@code ObjectManager}.
	 */
	private static final long POLL_INVERSE_SERVICE_REQUEST_TIMEOUT_MS = 15L * 1000L; // 15 seconds

	/**
	 * Timeout for {@link #performInverseServiceRequest(InverseServiceRequest)}.
	 * <p>
	 * If an inverse service-request does not receive a response within this timeout, a {@link TimeoutException} is thrown.
	 * <p>
	 * Please note, that the {@code invoke*} methods (e.g. {@link #invoke(Object, String, Object...)} or
	 * {@link #invokeConstructor(Class, Object...)}) can take much longer, because the other side will return
	 * a {@link DelayedMethodInvocationResponse} after a much shorter timeout (a few dozen seconds). This allows
	 * the actual method to be invoked to take how long it wants (unlimited!) while at the same time detecting very
	 * quickly, if the other side is dead (this timeout).
	 */
	private static final long PERFORM_INVERSE_SERVICE_REQUEST_TIMEOUT_MS = 3L * 60L * 1000L; // 3 minutes is more than enough, because we have DelayedMethodInvocationResponse

	private final IncDecRefCountQueue incDecRefCountQueue = new IncDecRefCountQueue(this);
	private final ObjectManager objectManager;
	private final LinkedList<InverseServiceRequest> inverseServiceRequests = new LinkedList<>();
	private final Set<Uid> requestIdsWaitingForResponse = new HashSet<Uid>(); // synchronized by: requestId2InverseServiceResponse
	private final Map<Uid, InverseServiceResponse> requestId2InverseServiceResponse = new HashMap<Uid, InverseServiceResponse>();
	private final ClassInfoMap classInfoMap = new ClassInfoMap();

	private volatile boolean diedOfTimeout;

	public static InverseInvoker getInverseInvoker(final ObjectManager objectManager) {
		requireNonNull(objectManager, "objectManager");

		synchronized (objectManager) {
			InverseInvoker inverseInvoker = (InverseInvoker) objectManager.getContextObject(InverseInvoker.class.getName());
			if (inverseInvoker == null) {
				inverseInvoker = new InverseInvoker(objectManager);
				objectManager.putContextObject(InverseInvoker.class.getName(), inverseInvoker);
			}
			return inverseInvoker;
		}
	}

	private InverseInvoker(final ObjectManager objectManager) {
		this.objectManager = requireNonNull(objectManager, "objectManager");
	}

	@Override
	public ObjectManager getObjectManager() {
		return objectManager;
	}

	@Override
	public <T> T invokeStatic(final Class<?> clazz, final String methodName, final Object ... arguments) {
		requireNonNull(clazz, "clazz");
		requireNonNull(methodName, "methodName");
		return invokeStatic(clazz.getName(), methodName, (String[]) null, arguments);
	}

	@Override
	public <T> T invokeStatic(final String className, final String methodName, final Object ... arguments) {
		requireNonNull(className, "className");
		requireNonNull(methodName, "methodName");
		return invokeStatic(className, methodName, (String[]) null, arguments);
	}

	@Override
	public <T> T invokeStatic(final Class<?> clazz, final String methodName, final Class<?>[] argumentTypes, final Object ... arguments) {
		requireNonNull(clazz, "clazz");
		requireNonNull(methodName, "methodName");
		return invokeStatic(clazz.getName(), methodName, toClassNames(argumentTypes), arguments);
	}

	@Override
	public <T> T invokeStatic(final String className, final String methodName, final String[] argumentTypeNames, final Object ... arguments) {
		requireNonNull(className, "className");
		requireNonNull(methodName, "methodName");

		final MethodInvocationRequest methodInvocationRequest = MethodInvocationRequest.forStaticInvocation(
				className, methodName, argumentTypeNames, arguments);

		return invoke(methodInvocationRequest);
	}

	@Override
	public <T> T invokeConstructor(final Class<T> clazz, final Object ... arguments) {
		requireNonNull(clazz, "clazz");
		return invokeConstructor(clazz.getName(), (String[]) null, arguments);
	}

	@Override
	public <T> T invokeConstructor(final String className, final Object ... arguments) {
		requireNonNull(className, "className");
		return invokeConstructor(className, (String[]) null, arguments);
	}

	@Override
	public <T> T invokeConstructor(final Class<T> clazz, final Class<?>[] argumentTypes, final Object ... arguments) {
		requireNonNull(clazz, "clazz");
		return invokeConstructor(clazz.getName(), toClassNames(argumentTypes), arguments);
	}

	@Override
	public <T> T invokeConstructor(final String className, final String[] argumentTypeNames, final Object ... arguments) {
		requireNonNull(className, "className");

		final MethodInvocationRequest methodInvocationRequest = MethodInvocationRequest.forConstructorInvocation(
				className, argumentTypeNames, arguments);

		return invoke(methodInvocationRequest);
	}

	@Override
	public <T> T invoke(final Object object, final String methodName, final Object ... arguments) {
		requireNonNull(object, "object");
		requireNonNull(methodName, "methodName");

		if (!(object instanceof RemoteObjectProxy))
			throw new IllegalArgumentException("object is not an instance of RemoteObjectProxy!");

		return invoke(object, methodName, (Class<?>[]) null, arguments);
	}

	@Override
	public <T> T invoke(final Object object, final String methodName, final Class<?>[] argumentTypes, final Object... arguments) {
		requireNonNull(object, "object");
		requireNonNull(methodName, "methodName");
		return invoke(object, methodName, toClassNames(argumentTypes), arguments);
	}

	@Override
	public <T> T invoke(final Object object, final String methodName, final String[] argumentTypeNames, final Object... arguments) {
		requireNonNull(object, "object");
		requireNonNull(methodName, "methodName");

		final MethodInvocationRequest methodInvocationRequest = MethodInvocationRequest.forObjectInvocation(
				object, methodName, argumentTypeNames, arguments);

		return invoke(methodInvocationRequest);
	}

	private <T> T invoke(final MethodInvocationRequest methodInvocationRequest) {
		requireNonNull(methodInvocationRequest, "methodInvocationRequest");

		InverseMethodInvocationResponse inverseMethodInvocationResponse = performInverseServiceRequest(
				new InverseMethodInvocationRequest(methodInvocationRequest));

		requireNonNull(inverseMethodInvocationResponse, "inverseMethodInvocationResponse");

		MethodInvocationResponse methodInvocationResponse = inverseMethodInvocationResponse.getMethodInvocationResponse();

		while (methodInvocationResponse instanceof DelayedMethodInvocationResponse) {
			final DelayedMethodInvocationResponse dmir = (DelayedMethodInvocationResponse) methodInvocationResponse;
			final Uid delayedResponseId = dmir.getDelayedResponseId();

			inverseMethodInvocationResponse = performInverseServiceRequest(
					new InverseMethodInvocationRequest(delayedResponseId));

			requireNonNull(inverseMethodInvocationResponse, "inverseMethodInvocationResponse");

			methodInvocationResponse = inverseMethodInvocationResponse.getMethodInvocationResponse();
		}

		final Object result = methodInvocationResponse.getResult();
		return cast(result);
	}

	@Override
	public void incRefCount(final ObjectRef objectRef, final Uid refId) {
		incDecRefCountQueue.incRefCount(objectRef, refId);
	}

	@Override
	public void decRefCount(final ObjectRef objectRef, final Uid refId) {
		incDecRefCountQueue.decRefCount(objectRef, refId);
	}

	private String[] toClassNames(Class<?> ... classes) {
		final String[] classNames;
		if (classes == null)
			classNames = null;
		else {
			classNames = new String[classes.length];
			for (int i = 0; i < classes.length; i++)
				classNames[i] = classes[i].getName();
		}
		return classNames;
	}

	public Object getRemoteObjectProxyOrCreate(ObjectRef objectRef) {
		return objectManager.getRemoteObjectProxyManager().getRemoteObjectProxyOrCreate(objectRef, new RemoteObjectProxyFactory() {
			@Override
			public RemoteObjectProxy createRemoteObjectProxy(ObjectRef objectRef) {
				return _createRemoteObjectProxy(objectRef);
			}
		});
	}

	private RemoteObjectProxy _createRemoteObjectProxy(final ObjectRef objectRef) {
		final Class<?>[] interfaces = getInterfaces(objectRef);

		final ClassLoader classLoader = this.getClass().getClassLoader();
		return (RemoteObjectProxy) Proxy.newProxyInstance(classLoader, interfaces,
				new RemoteObjectProxyInvocationHandler(this, objectRef));
	}

	private Class<?>[] getInterfaces(final ObjectRef objectRef) {
		ClassInfo classInfo = classInfoMap.getClassInfo(objectRef.getClassId());
		if (classInfo == null) {
			classInfo = objectRef.getClassInfo();
			if (classInfo == null)
				throw new IllegalStateException("There is no ClassInfo in the ClassInfoMap and neither in the ObjectRef! " + objectRef);

			classInfoMap.putClassInfo(classInfo);
			objectRef.setClassInfo(null);
		}

		final ClassManager classManager = objectManager.getClassManager();
		final Set<String> interfaceNames = classInfo.getInterfaceNames();
		final List<Class<?>> interfaces = new ArrayList<>(interfaceNames.size() + 1);
		for (final String interfaceName : interfaceNames) {
			Class<?> iface = null;
			try {
				iface = classManager.getClassOrFail(interfaceName);
			} catch (RuntimeException x) {
				if (ExceptionUtil.getCause(x, ClassNotFoundException.class) == null)
					throw x;
			}
			if (iface != null)
				interfaces.add(iface);
		}
		interfaces.add(RemoteObjectProxy.class);
		return interfaces.toArray(new Class<?>[interfaces.size()]);
	}

	/**
	 * Invokes a service on the client-side.
	 * <p>
	 * Normally, a client initiates a request-response-cycle by sending a request to a server-side service and waiting
	 * for the response. In order to notify client-side listeners, we need the inverse, though: the server must invoke
	 * a service on the client-side. This can be easily done by sending an implementation of {@code InverseServiceRequest}
	 * to a {@code InverseServiceRequestHandler} implementation on the client-side using this method.
	 *
	 * @param request the request to be processed on the client-side. Must not be <code>null</code>.
	 * @return the response created and sent back by the client-side {@code InverseServiceRequestHandler}.
	 * @throws TimeoutException if this method did not receive a response within the timeout
	 * {@link #PERFORM_INVERSE_SERVICE_REQUEST_TIMEOUT_MS}.
	 */
	public <T extends InverseServiceResponse> T performInverseServiceRequest(final InverseServiceRequest request) throws TimeoutException {
		requireNonNull(request, "request");

		if (diedOfTimeout)
			throw new IllegalStateException(String.format("InverseInvoker[%s] died of timeout, already!", objectManager.getClientId()));

		final Uid requestId = request.getRequestId();
		requireNonNull(requestId, "request.requestId");

		synchronized (requestId2InverseServiceResponse) {
			if (!requestIdsWaitingForResponse.add(requestId))
				throw new IllegalStateException("requestId already queued: " + requestId);
		}
		try {
			synchronized (inverseServiceRequests) {
				inverseServiceRequests.add(request);
				inverseServiceRequests.notify();
			}

			// The request is pushed, hence from now on, we wait for the response until the timeout in PERFORM_INVERSE_SERVICE_REQUEST_TIMEOUT_MS.
			final long startTimestamp = nowAsMillis();

			synchronized (requestId2InverseServiceResponse) {
				boolean first = true;
				while (first || nowAsMillis() - startTimestamp < PERFORM_INVERSE_SERVICE_REQUEST_TIMEOUT_MS) {
					if (first)
						first = false;
					else {
						final long timeSpentTillNowMillis = nowAsMillis() - startTimestamp;
						final long waitTimeout = PERFORM_INVERSE_SERVICE_REQUEST_TIMEOUT_MS - timeSpentTillNowMillis;
						if (waitTimeout > 0) {
							try {
								requestId2InverseServiceResponse.wait(waitTimeout);
							} catch (InterruptedException e) {
								doNothing();
							}
						}
					}

					final InverseServiceResponse response = requestId2InverseServiceResponse.remove(requestId);
					if (response != null) {
						if (response instanceof NullResponse)
							return null;
						else if (response instanceof ErrorResponse) {
							final Error error = ((ErrorResponse) response).getError();
							RemoteExceptionUtil.throwOriginalExceptionIfPossible(error);
							throw new RemoteException(error);
						}
						else {
							@SuppressWarnings("unchecked")
							final T t = (T) response;
							return t;
						}
					}
				}
			}
		} finally {
			boolean requestWasStillWaiting;
			// in case, it was not yet polled, we make sure garbage does not pile up.
			synchronized (requestId2InverseServiceResponse) {
				requestWasStillWaiting = requestIdsWaitingForResponse.remove(requestId);

				// Make sure, no garbage is left over by removing this together with the requestId from requestIdsWaitingForResponse.
				requestId2InverseServiceResponse.remove(requestId);
			}

			if (requestWasStillWaiting) {
				synchronized (inverseServiceRequests) {
					inverseServiceRequests.remove(request);
				}
			}
		}

		if (request.isTimeoutDeadly())
			diedOfTimeout = true;

		throw new TimeoutException(String.format("InverseInvoker[%s] encountered timeout while waiting for response matching requestId=%s! diedOfTimeout=%s",
				objectManager.getClientId(), requestId, diedOfTimeout));
	}

	public InverseServiceRequest pollInverseServiceRequest() {
		final long startTimestamp = nowAsMillis();

		synchronized (inverseServiceRequests) {
			boolean first = true;
			while (first || nowAsMillis() - startTimestamp < POLL_INVERSE_SERVICE_REQUEST_TIMEOUT_MS) {
				if (first)
					first = false;
				else {
					final long timeSpentTillNowMillis = nowAsMillis() - startTimestamp;
					final long waitTimeout = POLL_INVERSE_SERVICE_REQUEST_TIMEOUT_MS - timeSpentTillNowMillis;
					if (waitTimeout > 0) {
						try {
							inverseServiceRequests.wait(waitTimeout);
						} catch (InterruptedException e) {
							doNothing();
						}
					}
				}

				final InverseServiceRequest request = inverseServiceRequests.poll();
				if (request != null)
					return request;
			};
		}
		return null;
	}

	public void pushInverseServiceResponse(final InverseServiceResponse response) {
		requireNonNull(response, "response");

		final Uid requestId = response.getRequestId();
		requireNonNull(requestId, "response.requestId");

		synchronized (requestId2InverseServiceResponse) {
			if (!requestIdsWaitingForResponse.contains(requestId))
				throw new IllegalArgumentException(String.format("response.requestId=%s does not match any waiting request!", requestId));

			requestId2InverseServiceResponse.put(requestId, response);
			requestId2InverseServiceResponse.notifyAll();
		}
	}

	@Override
	public ClassInfoMap getClassInfoMap() {
		return classInfoMap;
	}
}
