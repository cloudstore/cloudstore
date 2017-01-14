package co.codewizards.cloudstore.ls.client;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.Closeable;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.core.util.ExceptionUtil;
import co.codewizards.cloudstore.core.util.ReflectionUtil;
import co.codewizards.cloudstore.ls.client.handler.InverseServiceRequestHandlerThread;
import co.codewizards.cloudstore.ls.core.LsConfig;
import co.codewizards.cloudstore.ls.core.invoke.ClassInfo;
import co.codewizards.cloudstore.ls.core.invoke.ClassInfoMap;
import co.codewizards.cloudstore.ls.core.invoke.ClassManager;
import co.codewizards.cloudstore.ls.core.invoke.DelayedMethodInvocationResponse;
import co.codewizards.cloudstore.ls.core.invoke.IncDecRefCountQueue;
import co.codewizards.cloudstore.ls.core.invoke.Invoker;
import co.codewizards.cloudstore.ls.core.invoke.MethodInvocationRequest;
import co.codewizards.cloudstore.ls.core.invoke.MethodInvocationResponse;
import co.codewizards.cloudstore.ls.core.invoke.ObjectManager;
import co.codewizards.cloudstore.ls.core.invoke.ObjectRef;
import co.codewizards.cloudstore.ls.core.invoke.RemoteObjectProxy;
import co.codewizards.cloudstore.ls.core.invoke.RemoteObjectProxyFactory;
import co.codewizards.cloudstore.ls.core.invoke.RemoteObjectProxyInvocationHandler;
import co.codewizards.cloudstore.ls.core.provider.JavaNativeWithObjectRefMessageBodyReader;
import co.codewizards.cloudstore.ls.core.provider.JavaNativeWithObjectRefMessageBodyWriter;
import co.codewizards.cloudstore.ls.rest.client.LocalServerRestClient;
import co.codewizards.cloudstore.ls.rest.client.request.GetDelayedMethodInvocationResponse;
import co.codewizards.cloudstore.ls.rest.client.request.InvokeMethod;

/**
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public class LocalServerClient implements Invoker, Closeable {

	private static final Logger logger = LoggerFactory.getLogger(LocalServerClient.class);

	private volatile InverseServiceRequestHandlerThread inverseServiceRequestHandlerThread;

	private LocalServerRestClient localServerRestClient;
	private final ObjectManager objectManager = ObjectManager.getInstance(new Uid()); // needed for inverse references as used by listeners!
	{
		objectManager.setNeverEvict(true);
	}
	private final ClassInfoMap classInfoMap = new ClassInfoMap();

	private final IncDecRefCountQueue incDecRefCountQueue = new IncDecRefCountQueue(this);

	@Override
	public ClassInfoMap getClassInfoMap() {
		return classInfoMap;
	}

	private static final class Holder {
		public static final LocalServerClient instance = new LocalServerClient();
	}

	public static LocalServerClient getInstance() {
		return Holder.instance;
	}

	public final synchronized LocalServerRestClient getLocalServerRestClient() {
		if (localServerRestClient == null) {
			localServerRestClient = _getLocalServerRestClient();

			final ObjectRefConverterFactoryImpl objectRefConverterFactory = new ObjectRefConverterFactoryImpl(this);
			localServerRestClient.registerRestComponent(new JavaNativeWithObjectRefMessageBodyReader(objectRefConverterFactory));
			localServerRestClient.registerRestComponent(new JavaNativeWithObjectRefMessageBodyWriter(objectRefConverterFactory));
		}
		return localServerRestClient;
	}

	protected LocalServerRestClient _getLocalServerRestClient() {
		return LocalServerRestClient.getInstance();
	}

	protected LocalServerClient() {
		if (LsConfig.isLocalServerEnabled()) {
			inverseServiceRequestHandlerThread = new InverseServiceRequestHandlerThread(this);
			inverseServiceRequestHandlerThread.start();
		}
	}

	@Override
	public ObjectManager getObjectManager() {
		return objectManager;
	}

	@Override
	public <T> T invokeStatic(final Class<?> clazz, final String methodName, final Object ... arguments) {
		assertNotNull(clazz, "clazz");
		assertNotNull(methodName, "methodName");
		if (! LsConfig.isLocalServerEnabled())
			return ReflectionUtil.invokeStatic(clazz, methodName, arguments);

		return invokeStatic(clazz.getName(), methodName, (String[]) null, arguments);
	}

	@Override
	public <T> T invokeStatic(final String className, final String methodName, final Object ... arguments) {
		assertNotNull(className, "className");
		assertNotNull(methodName, "methodName");
		if (! LsConfig.isLocalServerEnabled())
			return ReflectionUtil.invokeStatic(getClassOrFail(className), methodName, arguments);

		return invokeStatic(className, methodName, (String[]) null, arguments);
	}

	@Override
	public <T> T invokeStatic(final Class<?> clazz, final String methodName, final Class<?>[] argumentTypes, final Object ... arguments) {
		assertNotNull(clazz, "clazz");
		assertNotNull(methodName, "methodName");
		if (! LsConfig.isLocalServerEnabled())
			return ReflectionUtil.invokeStatic(clazz, methodName, argumentTypes, arguments);

		return invokeStatic(clazz.getName(), methodName, toClassNames(argumentTypes), arguments);
	}

	@Override
	public <T> T invokeStatic(final String className, final String methodName, final String[] argumentTypeNames, final Object ... arguments) {
		assertNotNull(className, "className");
		assertNotNull(methodName, "methodName");
		if (! LsConfig.isLocalServerEnabled())
			return ReflectionUtil.invokeStatic(getClassOrFail(className), methodName, getClassesOrFail(argumentTypeNames), arguments);

		final MethodInvocationRequest methodInvocationRequest = MethodInvocationRequest.forStaticInvocation(
				className, methodName, argumentTypeNames, arguments);

		return invoke(methodInvocationRequest);
	}

	@Override
	public <T> T invokeConstructor(final Class<T> clazz, final Object ... arguments) {
		assertNotNull(clazz, "clazz");
		if (! LsConfig.isLocalServerEnabled())
			return ReflectionUtil.invokeConstructor(clazz, arguments);

		return invokeConstructor(clazz.getName(), (String[]) null, arguments);
	}

	@Override
	public <T> T invokeConstructor(final String className, final Object ... arguments) {
		assertNotNull(className, "className");
		if (! LsConfig.isLocalServerEnabled())
			return cast(ReflectionUtil.invokeConstructor(getClassOrFail(className), arguments));

		return invokeConstructor(className, (String[]) null, arguments);
	}

	@Override
	public <T> T invokeConstructor(final Class<T> clazz, final Class<?>[] argumentTypes, final Object ... arguments) {
		assertNotNull(clazz, "clazz");
		if (! LsConfig.isLocalServerEnabled())
			return ReflectionUtil.invokeConstructor(clazz, argumentTypes, arguments);

		return invokeConstructor(clazz.getName(), toClassNames(argumentTypes), arguments);
	}

	@Override
	public <T> T invokeConstructor(final String className, final String[] argumentTypeNames, final Object ... arguments) {
		assertNotNull(className, "className");
		if (! LsConfig.isLocalServerEnabled())
			return cast(ReflectionUtil.invokeConstructor(getClassOrFail(className), getClassesOrFail(argumentTypeNames), arguments));

		final MethodInvocationRequest methodInvocationRequest = MethodInvocationRequest.forConstructorInvocation(
				className, argumentTypeNames, arguments);

		return invoke(methodInvocationRequest);
	}

	@Override
	public <T> T invoke(final Object object, final String methodName, final Object ... arguments) {
		assertNotNull(object, "object");
		assertNotNull(methodName, "methodName");
		if (! LsConfig.isLocalServerEnabled())
			return cast(ReflectionUtil.invoke(object, methodName, arguments));

		if (!(object instanceof RemoteObjectProxy) && !(object instanceof Serializable))
			throw new IllegalArgumentException("object is neither an instance of RemoteObjectProxy nor Serializable!");

		return invoke(object, methodName, (Class<?>[]) null, arguments);
	}

	@Override
	public <T> T invoke(final Object object, final String methodName, final Class<?>[] argumentTypes, final Object... arguments) {
		assertNotNull(object, "object");
		assertNotNull(methodName, "methodName");
		if (! LsConfig.isLocalServerEnabled())
			return cast(ReflectionUtil.invoke(object, methodName, argumentTypes, arguments));

		return invoke(object, methodName, toClassNames(argumentTypes), arguments);
	}

	@Override
	public <T> T invoke(final Object object, final String methodName, final String[] argumentTypeNames, final Object... arguments) {
		assertNotNull(object, "object");
		assertNotNull(methodName, "methodName");
		if (! LsConfig.isLocalServerEnabled())
			return cast(ReflectionUtil.invoke(object, methodName, getClassesOrFail(argumentTypeNames), arguments));

		final MethodInvocationRequest methodInvocationRequest = MethodInvocationRequest.forObjectInvocation(
				object, methodName, argumentTypeNames, arguments);

		return invoke(methodInvocationRequest);
	}

	private Class<?>[] getClassesOrFail(final String[] classNames) {
		assertNotNull(classNames, "classNames");
		final Class<?>[] result = new Class<?>[classNames.length];
		for (int i = 0; i < classNames.length; i++)
			result[i] = getClassOrFail(classNames[i]);

		return result;
	}

	private Class<?> getClassOrFail(final String className) {
		assertNotNull(className, "className");
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		if (loader == null)
			loader = getClass().getClassLoader();

		try {
			return Class.forName(className, true, loader);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
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

	private <T> T invoke(final MethodInvocationRequest methodInvocationRequest) {
		assertNotNull(methodInvocationRequest, "methodInvocationRequest");

		MethodInvocationResponse methodInvocationResponse = getLocalServerRestClient().execute(
				new InvokeMethod(methodInvocationRequest));

		while (methodInvocationResponse instanceof DelayedMethodInvocationResponse) {
			final DelayedMethodInvocationResponse dmir = (DelayedMethodInvocationResponse) methodInvocationResponse;
			final Uid delayedResponseId = dmir.getDelayedResponseId();

			methodInvocationResponse = getLocalServerRestClient().execute(
					new GetDelayedMethodInvocationResponse(delayedResponseId));
		}

		final Object result = methodInvocationResponse.getResult();
		if (methodInvocationResponse.getWritableArguments() != null)
			copyWritableArgumentsBack(methodInvocationRequest.getArguments(), methodInvocationResponse.getWritableArguments());

		return cast(result);
	}

	private void copyWritableArgumentsBack(final Object[] requestArguments, final Object[] responseArguments) {
		assertNotNull(requestArguments, "requestArguments");
		assertNotNull(responseArguments, "responseArguments");

		for (int i = 0; i < responseArguments.length; ++i) {
			final Object responseArgument = responseArguments[i];
			if (responseArgument != null)
				copyWritableArgumentBack(requestArguments[i], responseArgument);
		}
	}

	private void copyWritableArgumentBack(final Object requestArgument, final Object responseArgument) {
		assertNotNull(requestArgument, "requestArgument");
		assertNotNull(responseArgument, "responseArgument");

		if (requestArgument.getClass().isArray()) {
			final int length = Array.getLength(requestArgument);
			for (int i = 0; i < length; ++i) {
				final Object value = Array.get(responseArgument, i);
				Array.set(requestArgument, i, value);
			}
		}
		else
			throw new UnsupportedOperationException("No idea how to copy this back! requestArgument=" + requestArgument);
	}

	private RemoteObjectProxy _createRemoteObjectProxy(final ObjectRef objectRef, final Class<?>[] interfaces) {
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

	@Override
	public void incRefCount(final ObjectRef objectRef, final Uid refId) {
		incDecRefCountQueue.incRefCount(objectRef, refId);
	}

	@Override
	public void decRefCount(final ObjectRef objectRef, final Uid refId) {
		incDecRefCountQueue.decRefCount(objectRef, refId);
	}

	@Override
	protected void finalize() throws Throwable {
		close();
		super.finalize();
	}

	@Override
	public void close() {
		final Thread thread = inverseServiceRequestHandlerThread;
		if (thread != null) {
			inverseServiceRequestHandlerThread = null;
			thread.interrupt();
			try {
				thread.join();
			} catch (InterruptedException e) {
				doNothing();
			}
		}

		objectManager.setNeverEvict(false);

		if (LsConfig.isLocalServerEnabled()) {
			try {
				invokeStatic(ObjectRef.class, ObjectRef.VIRTUAL_METHOD_CLOSE_OBJECT_MANAGER, (Class<?>[])null, (Object[]) null);
			} catch (Exception x) {
				logger.error("close: " + x, x);
			}
		}
	}

	public Object getRemoteObjectProxyOrCreate(final ObjectRef objectRef) {
		return objectManager.getRemoteObjectProxyManager().getRemoteObjectProxyOrCreate(objectRef, new RemoteObjectProxyFactory() {
			@Override
			public RemoteObjectProxy createRemoteObjectProxy(final ObjectRef objectRef) {
				final Class<?>[] interfaces = getInterfaces(objectRef);
				return _createRemoteObjectProxy(objectRef, interfaces);
			}
		});
	}
}
