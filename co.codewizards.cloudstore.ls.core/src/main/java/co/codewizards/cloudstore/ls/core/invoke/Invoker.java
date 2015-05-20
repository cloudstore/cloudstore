package co.codewizards.cloudstore.ls.core.invoke;

import java.beans.PropertyChangeListener;
import java.io.Serializable;
import java.lang.reflect.Proxy;

import co.codewizards.cloudstore.core.dto.Uid;

public interface Invoker {

	/**
	 * Invoke a static method from the {@code LocalServerClient} in the {@code LocalServer} or vice-versa.
	 * <p>
	 * Convenience method delegating to {@link #invokeStatic(String, String, String[], Object...)}.
	 * <p>
	 * See {@link #invoke(Object, String, String[], Object...)} for further details.
	 * @param clazz the class owning the static method to be invoked. Must not be <code>null</code>.
	 * @param methodName the name of the static method to be invoked. Must not be <code>null</code>.
	 * @param arguments the arguments passed to the static method. May be <code>null</code> (if the method does not take any parameters).
	 * @return the result of the method invocation. May be <code>null</code>.
	 * @see #invokeStatic(String, String, String[], Object...)
	 * @see #invoke(Object, String, String[], Object...)
	 */
	<T> T invokeStatic(Class<?> clazz, String methodName, Object... arguments);

	/**
	 * Invoke a static method from the {@code LocalServerClient} in the {@code LocalServer} or vice-versa.
	 * <p>
	 * Convenience method delegating to {@link #invokeStatic(String, String, String[], Object...)}.
	 * <p>
	 * See {@link #invoke(Object, String, String[], Object...)} for further details.
	 * @param className the fully qualified name of the class owning the static method to be invoked. Must not be <code>null</code>.
	 * @param methodName the name of the static method to be invoked. Must not be <code>null</code>.
	 * @param arguments the arguments passed to the static method. May be <code>null</code> (if the method does not take any parameters).
	 * @return the result of the method invocation. May be <code>null</code>.
	 * @see #invokeStatic(String, String, String[], Object...)
	 * @see #invoke(Object, String, String[], Object...)
	 */
	<T> T invokeStatic(String className, String methodName, Object... arguments);

	/**
	 * Invoke a static method from the {@code LocalServerClient} in the {@code LocalServer} or vice-versa.
	 * <p>
	 * Convenience method delegating to {@link #invokeStatic(String, String, String[], Object...)}.
	 * <p>
	 * See {@link #invoke(Object, String, String[], Object...)} for further details.
	 * @param clazz the class owning the static method to be invoked. Must not be <code>null</code>.
	 * @param methodName the name of the static method to be invoked. Must not be <code>null</code>.
	 * @param argumentTypes the argument-types. May be <code>null</code>; then a matching method
	 * will be searched. If there are multiple matching methods, an exception is thrown, though (and the argument-types must be
	 * specified). If {@code argumentTypes} is not <code>null</code>, its {@code length} must match the one of {@code arguments}.
	 * @param arguments the arguments passed to the static method. May be <code>null</code> (if the method does not take any parameters).
	 * @return the result of the method invocation. May be <code>null</code>.
	 * @see #invokeStatic(String, String, String[], Object...)
	 * @see #invoke(Object, String, String[], Object...)
	 */
	<T> T invokeStatic(Class<?> clazz, String methodName, Class<?>[] argumentTypes, Object... arguments);

	/**
	 * Invoke a static method from the {@code LocalServerClient} in the {@code LocalServer} or vice-versa.
	 * <p>
	 * See {@link #invoke(Object, String, String[], Object...)} for further details.
	 * @param className the fully qualified name of the class owning the static method to be invoked. Must not be <code>null</code>.
	 * @param methodName the name of the static method to be invoked. Must not be <code>null</code>.
	 * @param argumentTypeNames the fully qualified names of the argument-types. May be <code>null</code>; then a matching method
	 * will be searched. If there are multiple matching methods, an exception is thrown, though (and the argument-types must be
	 * specified). If {@code argumentTypeNames} is not <code>null</code>, its {@code length} must match the one of {@code arguments}.
	 * @param arguments the arguments passed to the static method. May be <code>null</code> (if the method does not take any parameters).
	 * @return the result of the method invocation. May be <code>null</code>.
	 * @see #invoke(Object, String, String[], Object...)
	 */
	<T> T invokeStatic(String className, String methodName, String[] argumentTypeNames, Object... arguments);


	/**
	 * Invoke a constructor from the {@code LocalServerClient} in the {@code LocalServer} or vice-versa.
	 * <p>
	 * Convenience method delegating to {@link #invokeConstructor(String, String[], Object...)}.
	 * <p>
	 * See {@link #invoke(Object, String, String[], Object...)} for further details.
	 * @param clazz the class to be instantiated. Must not be <code>null</code>.
	 * @param arguments the arguments passed to the constructor. May be <code>null</code> (if the constructor does not take any parameters).
	 * @return the newly created objectRef. Never <code>null</code>.
	 * @see #invokeConstructor(String, String[], Object...)
	 * @see #invoke(Object, String, String[], Object...)
	 */
	<T> T invokeConstructor(Class<?> clazz, Object... arguments);

	/**
	 * Invoke a constructor from the {@code LocalServerClient} in the {@code LocalServer} or vice-versa.
	 * <p>
	 * See {@link #invoke(Object, String, String[], Object...)} for further details.
	 * @param className the fully qualified name of the class to be instantiated. Must not be <code>null</code>.
	 * @param arguments the arguments passed to the constructor. May be <code>null</code> (if the constructor does not take any parameters).
	 * @return the newly created objectRef. Never <code>null</code>.
	 * @see #invokeConstructor(String, String[], Object...)
	 * @see #invoke(Object, String, String[], Object...)
	 */
	<T> T invokeConstructor(String className, Object... arguments);

	<T> T invokeConstructor(Class<?> clazz, Class<?>[] argumentTypes, Object... arguments);

	<T> T invokeConstructor(String className, String[] argumentTypeNames, Object... arguments);


	<T> T invoke(Object objectRef, String methodName, Object... arguments);

	<T> T invoke(Object objectRef, String methodName, Class<?>[] argumentTypes, Object... arguments);

	/**
	 * Invoke a method on the given {@code objectRef} (which is a proxy) in the {@code LocalServer} or in its client
	 * (from the respective other side).
	 * <p>
	 * The {@code LocalServer} might reside in the same JVM or in a separate JVM (on the same computer, hence "local").
	 * <p>
	 * When invoking a method, the {@code arguments} must be passed to the real objectRef on the other side (in the other
	 * JVM). Therefore, all primitives ({@code byte}, {@code long} etc.) as well as all objects implementing
	 * {@link Serializable} are serialized (via Java native serialisation), transmitted via a REST call and deserialized.
	 * <p>
	 * If, however, an objectRef passed as an argument is a proxy of a real objectRef in the server (no matter, if it
	 * implements {@code Serializable} or not), it is converted into an {@link Object} instead - and this reference
	 * is transmitted via REST. The server then resolves the {@link Object} to the real objectRef.
	 * <p>
	 * If an objectRef in the {@code arguments} is neither a proxy of a {@code LocalServer}-objectRef (it may be a proxy of
	 * sth. else) nor implements {@code Serializable}, instead a reverse-proxy is created on the server-side. Therefore, an
	 * {@link Object} in the local JVM is created and passed via REST. The server then determines all interfaces of
	 * the real objectRef and instantiates a proxy (or re-uses an already existing one). This reverse-proxy-mechanism allows
	 * for passing a listener, e.g. a {@link PropertyChangeListener}: If the server invokes a method on the reverse-proxy,
	 * the {@code InverseInvoker} is used to invoke the corresponding method on the real objectRef in the client-JVM.
	 * <p>
	 * <b>Important:</b> For the proxies (both the ones on the client-side and the reverse-ones on the server-side), the
	 * standard Java {@link Proxy} is used. Therefore, only interfaces can be proxied - no classes. We cannot use cglib
	 * or any other more advanced proxy-lib, because these libs cannot be used with Android.
	 * <p>
	 * However, if a method declared by a class and not an interface should be invoked, this can still be done via this
	 * method - it's just less convenient. Additionally, reverse-proxies (on the server-side) obviously can only be passed
	 * to the real objectRef's method, if the method-signature uses an interface (or {@code Object}) for the argument in question.
	 *
	 * @param objectRef the proxy on which to invoke a method. Must not be <code>null</code>. This proxy
	 * was returned by a previous invocation of one of the <i>invoke*</i> methods (which might have happened
	 * indirectly via an invocation of a proxy's method).
	 * @param methodName the name of the method to be invoked. Must not be <code>null</code>.
	 * @param argumentTypeNames the fully qualified names of the argument-types. May be <code>null</code>; then a matching method
	 * will be searched. If there are multiple matching methods, an exception is thrown, though (and the argument-types must be
	 * specified). If {@code argumentTypeNames} is not <code>null</code>, its {@code length} must match the one of {@code arguments}.
	 * @param arguments the arguments passed to the method. May be <code>null</code> (if the method does not take any parameters).
	 * @return the result of the method invocation. This is either a serialized and deserialized "simple" objectRef or a
	 * proxy for a more complex objectRef on the server-side.
	 */
	<T> T invoke(Object objectRef, String methodName, String[] argumentTypeNames, Object... arguments);

	void incRefCount(ObjectRef objectRef, Uid refId);

	void decRefCount(ObjectRef objectRef, Uid refId);
}
