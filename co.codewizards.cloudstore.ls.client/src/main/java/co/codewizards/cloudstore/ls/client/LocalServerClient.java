package co.codewizards.cloudstore.ls.client;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import java.beans.PropertyChangeListener;
import java.io.Closeable;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import co.codewizards.cloudstore.ls.core.provider.JavaNativeWithObjectRefMessageBodyReader;
import co.codewizards.cloudstore.ls.core.provider.JavaNativeWithObjectRefMessageBodyWriter;
import co.codewizards.cloudstore.ls.rest.client.LocalServerRestClient;
import co.codewizards.cloudstore.ls.rest.client.request.GetClassInfo;
import co.codewizards.cloudstore.ls.rest.client.request.InvokeMethod;

/**
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public class LocalServerClient implements Closeable {

	private volatile InverseServiceRequestHandlerThread inverseServiceRequestHandlerThread;

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

	/**
	 * Invoke a static method in the {@code LocalServer}.
	 * <p>
	 * Convenience method delegating to {@link #invokeStatic(String, String, Object...)}.
	 * <p>
	 * See {@link #invoke(Object, String, Object...)} for further details.
	 * @param clazz the class owning the static method to be invoked. Must not be <code>null</code>.
	 * @param methodName the name of the static method to be invoked. Must not be <code>null</code>.
	 * @param arguments the arguments passed to the static method. May be <code>null</code> (if the method does not take any parameters).
	 * @return the result of the method invocation. May be <code>null</code>.
	 * @see #invokeStatic(String, String, Object...)
	 * @see #invoke(Object, String, Object...)
	 */
	public <T> T invokeStatic(final Class<?> clazz, final String methodName, final Object ... arguments) {
		assertNotNull("clazz", clazz);
		assertNotNull("methodName", methodName);
		return invokeStatic(clazz.getName(), methodName, arguments);
	}

	/**
	 * Invoke a static method in the {@code LocalServer}.
	 * <p>
	 * See {@link #invoke(Object, String, Object...)} for further details.
	 * @param className the fully qualified name of the class owning the static method to be invoked. Must not be <code>null</code>.
	 * @param methodName the name of the static method to be invoked. Must not be <code>null</code>.
	 * @param arguments the arguments passed to the static method. May be <code>null</code> (if the method does not take any parameters).
	 * @return the result of the method invocation. May be <code>null</code>.
	 * @see #invoke(Object, String, Object...)
	 */
	public <T> T invokeStatic(final String className, final String methodName, final Object ... arguments) {
		assertNotNull("className", className);
		assertNotNull("methodName", methodName);
		final MethodInvocationRequest methodInvocationRequest = MethodInvocationRequest.forStaticInvocation(
				className, methodName, arguments);

		return invoke(methodInvocationRequest);
	}

	/**
	 * Invoke a constructor in the {@code LocalServer}.
	 * <p>
	 * Convenience method delegating to {@link #invokeConstructor(String, Object...)}.
	 * <p>
	 * See {@link #invoke(Object, String, Object...)} for further details.
	 * @param clazz the class to be instantiated. Must not be <code>null</code>.
	 * @param arguments the arguments passed to the constructor. May be <code>null</code> (if the constructor does not take any parameters).
	 * @return the newly created object. Never <code>null</code>.
	 * @see #invokeConstructor(String, Object...)
	 * @see #invoke(Object, String, Object...)
	 */
	public <T> T invokeConstructor(final Class<?> clazz, final Object ... arguments) {
		assertNotNull("clazz", clazz);
		return invokeConstructor(clazz.getName(), arguments);
	}

	/**
	 * Invoke a constructor in the {@code LocalServer}.
	 * <p>
	 * See {@link #invoke(Object, String, Object...)} for further details.
	 * @param className the fully qualified name of the class to be instantiated. Must not be <code>null</code>.
	 * @param arguments the arguments passed to the constructor. May be <code>null</code> (if the constructor does not take any parameters).
	 * @return the newly created object. Never <code>null</code>.
	 * @see #invoke(Object, String, Object...)
	 */
	public <T> T invokeConstructor(final String className, final Object ... arguments) {
		assertNotNull("className", className);
		final MethodInvocationRequest methodInvocationRequest = MethodInvocationRequest.forConstructorInvocation(
				className, arguments);

		return invoke(methodInvocationRequest);
	}

	/**
	 * Invoke a method on the given object (which is a proxy) in the {@code LocalServer}.
	 * <p>
	 * The {@code LocalServer} might reside in the same JVM or in a separate JVM (on the same computer, hence "local").
	 * <p>
	 * When invoking a method, the {@code arguments} must be passed to the real object on the other side (in the other
	 * JVM). Therefore, all primitives ({@code byte}, {@code long} etc.) as well as all objects implementing
	 * {@link Serializable} are serialized (via Java native serialisation), transmitted via a REST call and deserialized.
	 * <p>
	 * If, however, an object passed as an argument is a proxy of a real object in the server (no matter, if it
	 * implements {@code Serializable} or not), it is converted into an {@link ObjectRef} instead - and this reference
	 * is transmitted via REST. The server then resolves the {@link ObjectRef} to the real object.
	 * <p>
	 * If an object in the {@code arguments} is neither a proxy of a {@code LocalServer}-object (it may be a proxy of
	 * sth. else) nor implements {@code Serializable}, instead a reverse-proxy is created on the server-side. Therefore, an
	 * {@link ObjectRef} in the local JVM is created and passed via REST. The server then determines all interfaces of
	 * the real object and instantiates a proxy (or re-uses an already existing one). This reverse-proxy-mechanism allows
	 * for passing a listener, e.g. a {@link PropertyChangeListener}: If the server invokes a method on the reverse-proxy,
	 * the {@code InverseInvoker} is used to invoke the corresponding method on the real object in the client-JVM.
	 * <p>
	 * <b>Important:</b> For the proxies (both the ones on the client-side and the reverse-ones on the server-side), the
	 * standard Java {@link Proxy} is used. Therefore, only interfaces can be proxied - no classes. We cannot use cglib
	 * or any other more advanced proxy-lib, because these libs cannot be used with Android.
	 * <p>
	 * However, if a method declared by a class and not an interface should be invoked, this can still be done via this
	 * method - it's just less convenient. Additionally, reverse-proxies (on the server-side) obviously can only be passed
	 * to the real object's method, if the method-signature uses an interface (or {@code Object}) for the argument in question.
	 *
	 * @param object the proxy on which to invoke a method. Must not be <code>null</code>. This proxy
	 * was returned by a previous invocation of one of the <i>invoke*</i> methods (which might have happened
	 * indirectly via an invocation of a proxy's method).
	 * @param methodName the name of the method to be invoked. Must not be <code>null</code>.
	 * @param arguments the arguments passed to the method. May be <code>null</code> (if the method does not take any parameters).
	 * @return the result of the method invocation. This is either a serialized and deserialized "simple" object or a
	 * proxy for a more complex object on the server-side.
	 */
	public <T> T invoke(final Object object, final String methodName, final Object ... arguments) {
		assertNotNull("object", object);
		assertNotNull("methodName", methodName);

		if (!(object instanceof RemoteObjectProxy))
			throw new IllegalArgumentException("object is not an instance of RemoteObjectProxy!");

		return _invoke(object, methodName, (Class<?>[]) null, arguments);
	}

	private <T> T _invoke(final Object object, final String methodName, final Class<?>[] argumentTypes, final Object[] arguments) {
		assertNotNull("object", object);
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
				object, methodName, argumentTypeNames, arguments);

		return invoke(methodInvocationRequest);
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

	private static class RemoteObjectProxyInvocationHandler implements InvocationHandler {
		private static final Logger logger = LoggerFactory.getLogger(LocalServerClient.RemoteObjectProxyInvocationHandler.class);

		private final Uid refId = new Uid();
		private final LocalServerClient localServerClient;
		private final ObjectRef objectRef;

		public RemoteObjectProxyInvocationHandler(final LocalServerClient localServerClient, final ObjectRef objectRef) {
			this.localServerClient = assertNotNull("localServerClient", localServerClient);
			this.objectRef = assertNotNull("objectRef", objectRef);

			if (logger.isDebugEnabled())
				logger.debug("[{}]<init>: {} refId={}", getThisId(), objectRef, refId);

			localServerClient._invoke(objectRef, ObjectRef.VIRTUAL_METHOD_NAME_INC_REF_COUNT, (Class<?>[])null, new Object[] { refId });
		}

		@Override
		public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
			// BEGIN implement RemoteObjectProxy
			if ("getObjectRef".equals(method.getName()) && method.getParameterTypes().length == 0)
				return objectRef;
			// END implement RemoteObjectProxy

			if (logger.isDebugEnabled())
				logger.debug("[{}]invoke: method='{}'", getThisId(), method);

			return localServerClient._invoke(objectRef, method.getName(), method.getParameterTypes(), args);
		}

		@Override
		protected void finalize() throws Throwable {
			if (logger.isDebugEnabled())
				logger.debug("[{}]finalize: {}", getThisId(), objectRef);

			try {
				localServerClient._invoke(objectRef, ObjectRef.VIRTUAL_METHOD_NAME_DEC_REF_COUNT, (Class<?>[])null, new Object[] { refId });
			} catch (Exception x) {
				logger.warn("[" + getThisId() + "]finalize: " + x, x);
			}
			super.finalize();
		}

		private String getThisId() {
			return Integer.toHexString(System.identityHashCode(this));
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

	public Object getRemoteObjectProxyOrCreate(ObjectRef objectRef) {
		return objectManager.getRemoteObjectProxyManager().getRemoteObjectProxyOrCreate(objectRef, new RemoteObjectProxyFactory() {
			@Override
			public RemoteObjectProxy createRemoteObjectProxy(ObjectRef objectRef) {
				final Class<?>[] interfaces = getInterfaces(objectRef.getClassId());
				return _createRemoteObjectProxy(objectRef, interfaces);
			}
		});
	}
}
