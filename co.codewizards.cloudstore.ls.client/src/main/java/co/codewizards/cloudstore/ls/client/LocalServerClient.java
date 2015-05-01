package co.codewizards.cloudstore.ls.client;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.Closeable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.core.util.ExceptionUtil;
import co.codewizards.cloudstore.ls.client.handler.InverseServiceRequestHandlerThread;
import co.codewizards.cloudstore.ls.core.invoke.ClassInfo;
import co.codewizards.cloudstore.ls.core.invoke.ClassInfoCache;
import co.codewizards.cloudstore.ls.core.invoke.ClassManager;
import co.codewizards.cloudstore.ls.core.invoke.MethodInvocationRequest;
import co.codewizards.cloudstore.ls.core.invoke.MethodInvocationResponse;
import co.codewizards.cloudstore.ls.core.invoke.ObjectManager;
import co.codewizards.cloudstore.ls.core.invoke.ObjectRef;
import co.codewizards.cloudstore.ls.core.invoke.RemoteObjectProxy;
import co.codewizards.cloudstore.ls.core.invoke.RemoteObjectProxyFactory;
import co.codewizards.cloudstore.ls.rest.client.LocalServerRestClient;
import co.codewizards.cloudstore.ls.rest.client.request.GetClassInfo;
import co.codewizards.cloudstore.ls.rest.client.request.InvokeMethod;

public class LocalServerClient implements Closeable {

	private static final int INVERSE_SERVICE_REQUEST_HANDLER_THREADS_COUNT = 3;
	private final List<InverseServiceRequestHandlerThread> inverseServiceRequestHandlerThreads = new CopyOnWriteArrayList<>();

	private LocalServerRestClient localServerRestClient;
	private final ObjectManager objectManager = ObjectManager.getInstance(new Uid()); // needed for inverse references as used by listeners!
	{
		objectManager.setNeverEvict(true);
	}
	private final ClassInfoCache classInfoCache = new ClassInfoCache();

	public ClassInfoCache getClassInfoCache() {
		return classInfoCache;
	}

	private static final class Holder {
		public static final LocalServerClient instance = new LocalServerClient();
	}

	public static LocalServerClient getInstance() {
		return Holder.instance;
	}

	public synchronized LocalServerRestClient getLocalServerRestClient() {
		if (localServerRestClient == null)
			localServerRestClient = LocalServerRestClient.getInstance();

		return localServerRestClient;
	}

	protected LocalServerClient() {
		startInverseServiceRequestHandlerThreads();
	}

	private void startInverseServiceRequestHandlerThreads() {
		for (int i = 0; i < INVERSE_SERVICE_REQUEST_HANDLER_THREADS_COUNT; ++i) {
			final InverseServiceRequestHandlerThread thread = new InverseServiceRequestHandlerThread(this);
			inverseServiceRequestHandlerThreads.add(thread);
			thread.start();
		}
	}

	private void stopInverseServiceRequestHandlerThreads() {
		for (final InverseServiceRequestHandlerThread thread : inverseServiceRequestHandlerThreads) {
			thread.interrupt();
			inverseServiceRequestHandlerThreads.remove(thread);
		}
	}

	public ObjectManager getObjectManager() {
		return objectManager;
	}

	public <T> T invokeStatic(final Class<?> clazz, final String methodName, final Object ... arguments) {
		assertNotNull("clazz", clazz);
		assertNotNull("methodName", methodName);
		return invokeStatic(clazz.getName(), methodName, arguments);
	}

	public <T> T invokeStatic(final String className, final String methodName, final Object ... arguments) {
		assertNotNull("className", className);
		assertNotNull("methodName", methodName);
		final MethodInvocationRequest methodInvocationRequest = MethodInvocationRequest.forStaticInvocation(
				className, methodName, fromObjectsToObjectRefs(arguments));

		return invoke(methodInvocationRequest);
	}

	public <T> T invokeConstructor(final Class<?> clazz, final Object ... arguments) {
		assertNotNull("clazz", clazz);
		return invokeConstructor(clazz.getName(), arguments);
	}

	public <T> T invokeConstructor(final String className, final Object ... arguments) {
		assertNotNull("className", className);
		final MethodInvocationRequest methodInvocationRequest = MethodInvocationRequest.forConstructorInvocation(
				className, fromObjectsToObjectRefs(arguments));

		return invoke(methodInvocationRequest);
	}

	public <T> T invoke(final Object object, final String methodName, final Object ... arguments) {
		assertNotNull("object", object);
		assertNotNull("methodName", methodName);

		if (!(object instanceof RemoteObjectProxy))
			throw new IllegalArgumentException("object is not an instance of RemoteObjectProxy!");

		final ObjectRef objectRef = assertNotNull("object.getObjectRef()", ((RemoteObjectProxy)object).getObjectRef());
		return invoke(objectRef, methodName, (Class<?>[]) null, arguments);
	}

	private <T> T invoke(final ObjectRef objectRef, final String methodName, final Class<?>[] argumentTypes, final Object[] arguments) {
		assertNotNull("objectRef", objectRef);
		assertNotNull("methodName", methodName);

		final String[] argumentTypeNames;
		if (argumentTypes == null)
			argumentTypeNames = null;
		else {
			argumentTypeNames = new String[argumentTypes.length];
			for (int i = 0; i < argumentTypes.length; i++)
				argumentTypeNames[i] = argumentTypes[i].getName();
		}

		final MethodInvocationRequest methodInvocationRequest = MethodInvocationRequest.forObjectInvocation(
				objectRef, methodName, argumentTypeNames, fromObjectsToObjectRefs(arguments));

		return invoke(methodInvocationRequest);
	}

	private Object[] fromObjectsToObjectRefs(final Object[] objects) {
		if (objects == null)
			return objects;

		final Object[] result = new Object[objects.length];
		for (int i = 0; i < objects.length; i++) {
			final Object object = objects[i];
			if (object instanceof RemoteObjectProxy) {
				result[i] = assertNotNull("object.getObjectRef()", ((RemoteObjectProxy)object).getObjectRef());
			} else
				result[i] = objectManager.getObjectRefOrObject(object);
		}
		return result;
	}

	private <T> T invoke(final MethodInvocationRequest methodInvocationRequest) {
		assertNotNull("methodInvocationRequest", methodInvocationRequest);

		final MethodInvocationResponse methodInvocationResponse = getLocalServerRestClient().execute(
				new InvokeMethod(methodInvocationRequest));

		final Object result = methodInvocationResponse.getResult();
		if (result == null)
			return null;

		if (result instanceof ObjectRef) {
			final ObjectRef resultObjectRef = (ObjectRef) result;
			return cast(getRemoteObjectProxyOrCreate(resultObjectRef));
		}

		return cast(result);
	}

	private RemoteObjectProxy _createRemoteObjectProxy(final ObjectRef objectRef, final Class<?>[] interfaces) {
		final ClassLoader classLoader = this.getClass().getClassLoader();
		return (RemoteObjectProxy) Proxy.newProxyInstance(classLoader, interfaces, new InvocationHandler() {
			@Override
			public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
				// BEGIN implement RemoteObjectProxy
				if ("getObjectRef".equals(method.getName()) && method.getParameterTypes().length == 0)
					return objectRef;
				// END implement RemoteObjectProxy

				return LocalServerClient.this.invoke(objectRef, method.getName(), method.getParameterTypes(), args);
			}

			@Override
			protected void finalize() throws Throwable {
				LocalServerClient.this.invoke(objectRef, ObjectRef.VIRTUAL_METHOD_NAME_REMOVE_OBJECT_REF, (Class<?>[])null, (Object[])null);
				super.finalize();
			}
		});
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
		ClassInfo classInfo = getClassInfoCache().getClassInfo(classId);
		if (classInfo == null) {
			classInfo = getLocalServerRestClient().execute(new GetClassInfo(classId));
			if (classInfo != null)
				getClassInfoCache().putClassInfo(classInfo);
		}
		return classInfo;
	}

	@Override
	protected void finalize() throws Throwable {
		close();
		super.finalize();
	}

	@Override
	public void close() {
		objectManager.setNeverEvict(false);
		stopInverseServiceRequestHandlerThreads();
	}

	public Object getRemoteObjectProxyOrCreate(ObjectRef objectRef) {
		return objectManager.getRemoteObjectProxyManager().getRemoteObjectProxy(objectRef, new RemoteObjectProxyFactory() {
			@Override
			public RemoteObjectProxy createRemoteObject(ObjectRef objectRef) {
				final Class<?>[] interfaces = getInterfaces(objectRef.getClassId());
				return _createRemoteObjectProxy(objectRef, interfaces);
			}
		});
	}
}
