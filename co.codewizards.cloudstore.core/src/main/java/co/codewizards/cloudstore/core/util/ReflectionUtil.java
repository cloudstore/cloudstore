package co.codewizards.cloudstore.core.util;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ReflectionUtil {

	private ReflectionUtil() { }

//	public static Class<?> getCallerClass() {
//		// TODO try to use sun.reflect.Reflection.getCallerClass(), if it's in the classpath.
//
//		final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
//		assertNotNull("Thread.currentThread.stackTrace", stackTrace);
//		final int stackTraceElementIndex = 2;
//
//		if (stackTrace.length < stackTraceElementIndex + 1)
//			throw new IllegalStateException(String.format("Thread.currentThread.stackTrace.length < %s", stackTraceElementIndex + 1));
//
//		final StackTraceElement stackTraceElement = stackTrace[stackTraceElementIndex];
//		final String className = stackTraceElement.getClassName();
//
//	}

	public static <T> T invokeConstructor(final Class<?> clazz, final Object ... args) {
		assertNotNull("clazz", clazz);

		final Class<?>[] argTypes = getArgumentTypes(args);

		final List<Constructor<?>> compatibleConstructors = new ArrayList<>();
		for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
			if (isConstructorCompatible(constructor, argTypes))
				compatibleConstructors.add(constructor);
		}

		if (compatibleConstructors.isEmpty()) {
			final String methodNameWithParameterTypes = createMethodNameWithParameterTypes(clazz.getSimpleName(), argTypes);
			throw new IllegalArgumentException(new NoSuchMethodException(String.format("None of the constructors of %s matches %s (or an equivalent using super-types of these parameter-types)!", clazz.getName(), methodNameWithParameterTypes)));
		}

		if (compatibleConstructors.size() > 1) {
			// TODO find + invoke the most suitable instead of throwing this exception!
			final String methodNameWithParameterTypes = createMethodNameWithParameterTypes(clazz.getSimpleName(), argTypes);
			throw new IllegalArgumentException(new NoSuchMethodException(String.format("%s declare multiple constructors matching %s (or an equivalent using super-types of these parameter-types)!", clazz.getName(), methodNameWithParameterTypes)));
		}

		return cast(invoke(compatibleConstructors.get(0), args));
	}

	public static <T> T invokeStatic(final Class<?> clazz, final String methodName, final Object ... args) {
		assertNotNull("clazz", clazz);
		assertNotNull("methodName", methodName);
		return invoke(clazz, (Object)null, methodName, args);
	}

	public static <T> T invoke(final Object object, final String methodName, final Object ... args) {
		assertNotNull("object", object);
		assertNotNull("methodName", methodName);
		return invoke(object.getClass(), object, methodName, args);
	}

	private static <T> T invoke(final Class<?> clazz, final Object object, final String methodName, final Object ... args) {
		assertNotNull("clazz", clazz);
		// object may be null
		assertNotNull("methodName", methodName);
		// args may be null

		final Class<?>[] argTypes = getArgumentTypes(args);

		// TODO cache methods - don't search for them again and again
		final List<Method> methods = getDeclaredMethods(clazz, methodName);
		final List<Method> compatibleMethods = new ArrayList<>(Math.min(5, methods.size()));
		for (final Method method : methods) {
			if (isMethodCompatible(method, argTypes))
				compatibleMethods.add(method);
		}

		if (compatibleMethods.isEmpty()) {
			final String methodNameWithParameterTypes = createMethodNameWithParameterTypes(methodName, argTypes);
			throw new IllegalArgumentException(new NoSuchMethodException(String.format("Neither %s nor one of its super-classes declares the method %s (or an equivalent using super-types of these parameter-types)!", clazz.getName(), methodNameWithParameterTypes)));
		}

		if (compatibleMethods.size() > 1) {
			// TODO find + invoke the most suitable instead of throwing this exception!
			final String methodNameWithParameterTypes = createMethodNameWithParameterTypes(methodName, argTypes);
			throw new IllegalArgumentException(new NoSuchMethodException(String.format("%s and its super-classes declare multiple methods matching %s (or an equivalent using super-types of these parameter-types)!", clazz.getName(), methodNameWithParameterTypes)));
		}

		return invoke(object, compatibleMethods.get(0), args);
	}

	private static Class<?>[] getArgumentTypes(final Object... args) {
		final Class<?>[] argTypes = args == null ? new Class<?>[0] : new Class<?>[args.length];
		for (int i = 0; i < argTypes.length; i++)
			argTypes[i] = args[i] == null ? null : args[i].getClass();
		return argTypes;
	}

	public static <T> T invoke(final Object object, final String methodName, Class<?>[] parameterTypes, final Object ... args) {
		assertNotNull("object", object);
		assertNotNull("methodName", methodName);

		if (parameterTypes == null)
			return invoke(object, methodName, args);

		final Method method = getDeclaredMethodOrFail(object.getClass(), methodName, parameterTypes);
		return invoke(object, method, args);
	}

	private static <T> T invoke(final Constructor<T> constructor, final Object ... args) {
		try {
			constructor.setAccessible(true);

			final Object result = constructor.newInstance(args);
			return cast(result);
		} catch (IllegalArgumentException e) {
			throw e;
		} catch (IllegalAccessException | InstantiationException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e.getCause());
		}
	}

	private static <T> T invoke(final Object object, Method method, final Object ... args) {
		try {
			method.setAccessible(true);

			final Object result = method.invoke(object, args);
			return cast(result);
		} catch (IllegalArgumentException e) {
			throw e;
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e.getCause());
		}
	}

	private static boolean isConstructorCompatible(final Constructor constructor, final Class<?>[] argTypes) {
		final Class<?>[] parameterTypes = constructor.getParameterTypes();
		return areMethodParametersCompatible(parameterTypes, argTypes);
	}

	private static boolean isMethodCompatible(final Method method, final Class<?>[] argTypes) {
		final Class<?>[] parameterTypes = method.getParameterTypes();
		return areMethodParametersCompatible(parameterTypes, argTypes);
	}

	private static boolean areMethodParametersCompatible(final Class<?>[] parameterTypes, final Class<?>[] argTypes) {
		if (argTypes.length != parameterTypes.length)
			return false;

		for (int i = 0; i < parameterTypes.length; i++) {
			if (argTypes[i] != null && !parameterTypes[i].isAssignableFrom(argTypes[i]))
				return false;
		}

		return true;
	}

	public static List<Method> getDeclaredMethods(final Class<?> clazz, final String name) {
		final List<Method> result = new ArrayList<>();

		Class<?> c = clazz;
		while (c != null) {
			final Method[] methods = c.getDeclaredMethods();
			for (final Method method : methods) {
				if (name.equals(method.getName()))
					result.add(method);
			}
			c = c.getSuperclass();
		}

		return result;
	}

	public static Method getDeclaredMethodOrFail(final Class<?> clazz, final String name, final Class<?>[] parameterTypes) {
		final Method method = getDeclaredMethod(clazz, name, parameterTypes);
		if (method == null) {
			final String methodNameWithParameterTypes = createMethodNameWithParameterTypes(name, parameterTypes);
			throw new IllegalArgumentException(new NoSuchMethodException(String.format("Neither %s nor one of its super-classes declares the method %s!", clazz.getName(), methodNameWithParameterTypes)));
		}
		return method;
	}

	private static String createMethodNameWithParameterTypes(final String name, final Class<?>[] parameterTypes) {
		final StringBuilder sb = new StringBuilder();
		for (Class<?> parameterType : parameterTypes) {
			if (sb.length() > 0)
				sb.append(", ");

			sb.append(parameterType.getName());
		}
		return name + '(' + sb.toString() + ')';
	}

	public static Method getDeclaredMethod(final Class<?> clazz, final String name, final Class<?>[] parameterTypes) {
		Class<?> c = clazz;
		while (c != null) {
			try {
				final Method declaredMethod = c.getDeclaredMethod(name, parameterTypes);
				return declaredMethod;
			} catch (NoSuchMethodException x) {
				doNothing(); // expected in many cases ;-)
			}
			c = c.getSuperclass();
		}
		return null;
	}

	public static List<Field> getAllDeclaredFields(final Class<?> clazz) {
		final List<Field> result = new ArrayList<>();
		Class<?> c = clazz;
		while (c != null) {
			final Field[] declaredFields = c.getDeclaredFields();
			for (Field field : declaredFields)
				result.add(field);

			c = c.getSuperclass();
		}
		return result;
	}

	public static Map<Field, Object> getAllDeclaredFieldValues(final Object object) {
		assertNotNull("object", object);

		final List<Field> allDeclaredFields = getAllDeclaredFields(object.getClass());
		final Map<Field, Object> result = new HashMap<>(allDeclaredFields.size());
		for (Field field : allDeclaredFields) {
			field.setAccessible(true);
			try {
				final Object fieldValue = field.get(object);
				result.put(field, fieldValue);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}
		return result;
	}

	public static void setFieldValue(final Object object, final String fieldName, final Object value) {
		// TODO very inefficient implementation - make better!

		String className = null;
		String simpleFieldName = fieldName;

		final int lastDotIndex = fieldName.lastIndexOf('.');
		if (lastDotIndex >= 0) {
			className = fieldName.substring(0, lastDotIndex);
			simpleFieldName = fieldName.substring(lastDotIndex + 1);
		}

		final List<Field> declaredFields = getAllDeclaredFields(object.getClass());
		for (final Field field : declaredFields) {
			if (className != null && !className.equals(field.getDeclaringClass().getName()))
				continue;

			if (!simpleFieldName.equals(field.getName()))
				continue;

			field.setAccessible(true);
			try {
				field.set(object, value);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
			return;
		}

		throw new IllegalArgumentException("object's class does not have this field: " + fieldName);
	}

	public static Set<Class<?>> getAllInterfaces(final Class<?> clazz) {
		assertNotNull("clazz", clazz);

		final Set<Class<?>> interfaces = new LinkedHashSet<>();

		Class<?> c = clazz;
		while (c != null) {
			populateInterfaces(interfaces, c);
			c = c.getSuperclass();
		}
		return interfaces;
	}

	private static void populateInterfaces(Collection<Class<?>> interfaces, Class<?> clazz) {
		for (final Class<?> iface : clazz.getInterfaces())
			interfaces.add(iface);

		for (final Class<?> iface : clazz.getInterfaces())
			populateInterfaces(interfaces, iface);
	}
}
