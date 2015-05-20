package co.codewizards.cloudstore.ls.client;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.Closeable;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.core.util.ExceptionUtil;
import co.codewizards.cloudstore.ls.client.handler.InverseServiceRequestHandlerThread;
import co.codewizards.cloudstore.ls.core.invoke.ClassInfo;
import co.codewizards.cloudstore.ls.core.invoke.ClassInfoMap;
import co.codewizards.cloudstore.ls.core.invoke.ClassManager;
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
import co.codewizards.cloudstore.ls.rest.client.request.InvokeMethod;

/**
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public class LocalServerClient implements Invoker, Closeable {

	private volatile InverseServiceRequestHandlerThread inverseServiceRequestHandlerThread;

	private LocalServerRestClient localServerRestClient;
	private final ObjectManager objectManager = ObjectManager.getInstance(new Uid()); // needed for inverse references as used by listeners!
	{
		objectManager.setNeverEvict(true);
	}
	private final ClassInfoMap classInfoMap = new ClassInfoMap();

	private final IncDecRefCountQueue incDecRefCountQueue = new IncDecRefCountQueue(this);

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
		inverseServiceRequestHandlerThread = new InverseServiceRequestHandlerThread(this);
		inverseServiceRequestHandlerThread.start();
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
		assertNotNull("methodInvocationRequest", methodInvocationRequest);

		final MethodInvocationResponse methodInvocationResponse = getLocalServerRestClient().execute(
				new InvokeMethod(methodInvocationRequest));

		final Object result = methodInvocationResponse.getResult();
		return cast(result);
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
		objectManager.setNeverEvict(false);

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
