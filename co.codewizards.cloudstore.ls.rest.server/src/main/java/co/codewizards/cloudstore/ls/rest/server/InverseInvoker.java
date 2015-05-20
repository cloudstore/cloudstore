package co.codewizards.cloudstore.ls.rest.server;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.dto.Error;
import co.codewizards.cloudstore.core.dto.RemoteException;
import co.codewizards.cloudstore.core.dto.RemoteExceptionUtil;
import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.core.io.TimeoutException;
import co.codewizards.cloudstore.core.util.ExceptionUtil;
import co.codewizards.cloudstore.ls.core.dto.ErrorResponse;
import co.codewizards.cloudstore.ls.core.dto.InverseServiceRequest;
import co.codewizards.cloudstore.ls.core.dto.InverseServiceResponse;
import co.codewizards.cloudstore.ls.core.dto.NullResponse;
import co.codewizards.cloudstore.ls.core.invoke.AbstractRemoteObjectProxyInvocationHandler;
import co.codewizards.cloudstore.ls.core.invoke.ClassInfo;
import co.codewizards.cloudstore.ls.core.invoke.ClassInfoCache;
import co.codewizards.cloudstore.ls.core.invoke.ClassManager;
import co.codewizards.cloudstore.ls.core.invoke.GetClassInfoRequest;
import co.codewizards.cloudstore.ls.core.invoke.GetClassInfoResponse;
import co.codewizards.cloudstore.ls.core.invoke.InverseMethodInvocationRequest;
import co.codewizards.cloudstore.ls.core.invoke.InverseMethodInvocationResponse;
import co.codewizards.cloudstore.ls.core.invoke.Invoker;
import co.codewizards.cloudstore.ls.core.invoke.MethodInvocationRequest;
import co.codewizards.cloudstore.ls.core.invoke.MethodInvocationResponse;
import co.codewizards.cloudstore.ls.core.invoke.ObjectManager;
import co.codewizards.cloudstore.ls.core.invoke.ObjectRef;
import co.codewizards.cloudstore.ls.core.invoke.RemoteObjectProxy;
import co.codewizards.cloudstore.ls.core.invoke.RemoteObjectProxyFactory;

public class InverseInvoker implements Invoker {
	/**
	 * Timeout (in milliseconds) before sending an empty HTTP response to the polling client. The client does
	 * <i>long polling</i> in order to allow for
	 * {@linkplain #performInverseServiceRequest(InverseServiceRequest) inverse service invocations}.
	 * <p>
	 * This timeout must be (significantly) shorter than {@link ObjectManager#TIMEOUT_EVICT_UNUSED_OBJECT_MANAGER_MS} to make sure, the
	 * {@linkplain #pollInverseServiceRequest() polling} serves additionally as a keep-alive for
	 * the server-side {@code ObjectManager}.
	 */
	private static final long POLL_INVERSE_SERVICE_REQUEST_TIMEOUT_MS = 10L * 1000L; // 10 seconds

	/**
	 * Timeout for {@link #performInverseServiceRequest(InverseServiceRequest)}.
	 * <p>
	 * If an inverse service-request does not receive a response within this timeout, a {@link TimeoutException} is thrown.
	 */
	private static final long PERFORM_INVERSE_SERVICE_REQUEST_TIMEOUT_MS = 15L * 60L * 1000L; // 15 minutes

	private final ObjectManager objectManager;
	private final LinkedList<InverseServiceRequest> inverseServiceRequests = new LinkedList<>();
	private final Set<Uid> requestIdsWaitingForResponse = new HashSet<Uid>(); // synchronized by: requestId2InverseServiceResponse
	private final Map<Uid, InverseServiceResponse> requestId2InverseServiceResponse = new HashMap<Uid, InverseServiceResponse>();
	private final ClassInfoCache classInfoCache = new ClassInfoCache();

	public static InverseInvoker getInverseInvoker(final ObjectManager objectManager) {
		assertNotNull("objectManager", objectManager);

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
		this.objectManager = assertNotNull("objectManager", objectManager);
	}

	public ObjectManager getObjectManager() {
		return objectManager;
	}

	@Override
	public <T> T invokeStatic(final Class<?> clazz, final String methodName, final Object ... arguments) {
		assertNotNull("clazz", clazz);
		assertNotNull("methodName", methodName);
		return invokeStatic(clazz.getName(), methodName, (String[]) null, arguments);
	}

	@Override
	public <T> T invokeStatic(final String className, final String methodName, final Object ... arguments) {
		assertNotNull("className", className);
		assertNotNull("methodName", methodName);
		return invokeStatic(className, methodName, (String[]) null, arguments);
	}

	@Override
	public <T> T invokeStatic(final Class<?> clazz, final String methodName, final Class<?>[] argumentTypes, final Object ... arguments) {
		assertNotNull("clazz", clazz);
		assertNotNull("methodName", methodName);
		return invokeStatic(clazz.getName(), methodName, toClassNames(argumentTypes), arguments);
	}

	@Override
	public <T> T invokeStatic(final String className, final String methodName, final String[] argumentTypeNames, final Object ... arguments) {
		assertNotNull("className", className);
		assertNotNull("methodName", methodName);

		final MethodInvocationRequest methodInvocationRequest = MethodInvocationRequest.forStaticInvocation(
				className, methodName, argumentTypeNames, arguments);

		return invoke(methodInvocationRequest);
	}

	@Override
	public <T> T invokeConstructor(final Class<?> clazz, final Object ... arguments) {
		assertNotNull("clazz", clazz);
		return invokeConstructor(clazz.getName(), (String[]) null, arguments);
	}

	@Override
	public <T> T invokeConstructor(final String className, final Object ... arguments) {
		assertNotNull("className", className);
		return invokeConstructor(className, (String[]) null, arguments);
	}

	@Override
	public <T> T invokeConstructor(final Class<?> clazz, final Class<?>[] argumentTypes, final Object ... arguments) {
		assertNotNull("clazz", clazz);
		return invokeConstructor(clazz.getName(), toClassNames(argumentTypes), arguments);
	}

	@Override
	public <T> T invokeConstructor(final String className, final String[] argumentTypeNames, final Object ... arguments) {
		assertNotNull("className", className);

		final MethodInvocationRequest methodInvocationRequest = MethodInvocationRequest.forConstructorInvocation(
				className, argumentTypeNames, arguments);

		return invoke(methodInvocationRequest);
	}

	@Override
	public <T> T invoke(final Object object, final String methodName, final Object ... arguments) {
		assertNotNull("object", object);
		assertNotNull("methodName", methodName);

		if (!(object instanceof RemoteObjectProxy))
			throw new IllegalArgumentException("object is not an instance of RemoteObjectProxy!");

		return invoke(object, methodName, (Class<?>[]) null, arguments);
	}

	@Override
	public <T> T invoke(final Object object, final String methodName, final Class<?>[] argumentTypes, final Object... arguments) {
		assertNotNull("object", object);
		assertNotNull("methodName", methodName);
		return invoke(object, methodName, toClassNames(argumentTypes), arguments);
	}

	@Override
	public <T> T invoke(final Object object, final String methodName, final String[] argumentTypeNames, final Object... arguments) {
		assertNotNull("object", object);
		assertNotNull("methodName", methodName);

		final MethodInvocationRequest methodInvocationRequest = MethodInvocationRequest.forObjectInvocation(
				object, methodName, argumentTypeNames, arguments);

		return invoke(methodInvocationRequest);
	}

	private <T> T invoke(final MethodInvocationRequest methodInvocationRequest) {
		assertNotNull("methodInvocationRequest", methodInvocationRequest);

		final InverseMethodInvocationResponse inverseMethodInvocationResponse = performInverseServiceRequest(
				new InverseMethodInvocationRequest(methodInvocationRequest));

		assertNotNull("inverseMethodInvocationResponse", inverseMethodInvocationResponse);

		final MethodInvocationResponse methodInvocationResponse = inverseMethodInvocationResponse.getMethodInvocationResponse();

		final Object result = methodInvocationResponse.getResult();
		return cast(result);
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
		final Class<?>[] interfaces = getInterfaces(objectRef.getClassId());

		final ClassLoader classLoader = this.getClass().getClassLoader();
		return (RemoteObjectProxy) Proxy.newProxyInstance(classLoader, interfaces,
				new RemoteObjectProxyInvocationHandler(this, objectRef));
	}

	private static class RemoteObjectProxyInvocationHandler extends AbstractRemoteObjectProxyInvocationHandler {
		private static final Logger logger = LoggerFactory.getLogger(InverseInvoker.RemoteObjectProxyInvocationHandler.class);

		private final InverseInvoker inverseInvoker;

		public RemoteObjectProxyInvocationHandler(final InverseInvoker inverseInvoker, final ObjectRef objectRef) {
			super(objectRef);
			this.inverseInvoker = assertNotNull("inverseInvoker", inverseInvoker);

			if (logger.isDebugEnabled())
				logger.debug("[{}]<init>: {}", getThisId(), objectRef);

			inverseInvoker.invoke(objectRef, ObjectRef.VIRTUAL_METHOD_NAME_INC_REF_COUNT, (Class<?>[])null, new Object[] { refId });
		}

		@Override
		protected Object doInvoke(Object proxy, Method method, Object[] args) throws Throwable {
			return inverseInvoker.invoke(objectRef, method.getName(), method.getParameterTypes(), args);
		}

		@Override
		protected void finalize() throws Throwable {
			if (logger.isDebugEnabled())
				logger.debug("[{}]finalize: {}", getThisId(), objectRef);

			try {
				inverseInvoker.invoke(objectRef, ObjectRef.VIRTUAL_METHOD_NAME_DEC_REF_COUNT, (Class<?>[])null, new Object[] { refId });
			} catch (Exception x) {
				logger.warn("[" + getThisId() + "]finalize: " + x, x);
			}
			super.finalize();
		}
	}

	private Class<?>[] getInterfaces(int classId) {
		final ClassInfo classInfo = getClassInfoOrFail(classId);
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

	private ClassInfo getClassInfoOrFail(final int classId) {
		final ClassInfo classInfo = getClassInfo(classId);
		if (classInfo == null)
			throw new IllegalArgumentException("No ClassInfo found for classId=" + classId);

		return classInfo;
	}

	private ClassInfo getClassInfo(final int classId) {
		ClassInfo classInfo = classInfoCache.getClassInfo(classId);
		if (classInfo == null) {
			final GetClassInfoResponse getClassInfoResponse = performInverseServiceRequest(new GetClassInfoRequest(classId));
			if (getClassInfoResponse != null) {
				classInfo = getClassInfoResponse.getClassInfo();
				classInfoCache.putClassInfo(classInfo);
			}
		}
		return classInfo;
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
		assertNotNull("request", request);

		final Uid requestId = request.getRequestId();
		assertNotNull("request.requestId", requestId);

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
			final long startTimestamp = System.currentTimeMillis();

			synchronized (requestId2InverseServiceResponse) {
				boolean first = true;
				while (first || System.currentTimeMillis() - startTimestamp < PERFORM_INVERSE_SERVICE_REQUEST_TIMEOUT_MS) {
					if (first)
						first = false;
					else {
						final long timeSpentTillNowMillis = System.currentTimeMillis() - startTimestamp;
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

		throw new TimeoutException(String.format("Timeout waiting for response matching requestId=%s!", requestId));
	}

	public InverseServiceRequest pollInverseServiceRequest() {
		final long startTimestamp = System.currentTimeMillis();

		synchronized (inverseServiceRequests) {
			boolean first = true;
			while (first || System.currentTimeMillis() - startTimestamp < POLL_INVERSE_SERVICE_REQUEST_TIMEOUT_MS) {
				if (first)
					first = false;
				else {
					final long timeSpentTillNowMillis = System.currentTimeMillis() - startTimestamp;
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
		assertNotNull("response", response);

		final Uid requestId = response.getRequestId();
		assertNotNull("response.requestId", requestId);

		synchronized (requestId2InverseServiceResponse) {
			if (!requestIdsWaitingForResponse.contains(requestId))
				throw new IllegalArgumentException(String.format("response.requestId=%s does not match any waiting request!", requestId));

			requestId2InverseServiceResponse.put(requestId, response);
			requestId2InverseServiceResponse.notifyAll();
		}
	}
}
