package co.codewizards.cloudstore.ls.core.invoke;

public interface Invoker {

	<T> T invokeStatic(Class<?> clazz, String methodName, Object... arguments);

	<T> T invokeStatic(String className, String methodName, Object... arguments);

	<T> T invokeStatic(Class<?> clazz, String methodName, Class<?>[] argumentTypes, Object... arguments);

	<T> T invokeStatic(String className, String methodName, String[] argumentTypeNames, Object... arguments);


	<T> T invokeConstructor(Class<?> clazz, Object... arguments);

	<T> T invokeConstructor(String className, Object... arguments);

	<T> T invokeConstructor(Class<?> clazz, Class<?>[] argumentTypes, Object... arguments);

	<T> T invokeConstructor(String className, String[] argumentTypeNames, Object... arguments);


	<T> T invoke(Object object, String methodName, Object... arguments);

	<T> T invoke(Object object, String methodName, Class<?>[] argumentTypes, Object... arguments);

	<T> T invoke(Object object, String methodName, String[] argumentTypeNames, Object... arguments);
}
