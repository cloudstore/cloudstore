package co.codewizards.cloudstore.core.util;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReflectionUtil {

	private static final Logger logger = LoggerFactory.getLogger(ReflectionUtil.class);

	private ReflectionUtil() { }

	public static <T> T invokeConstructor(final Class<T> clazz, final Object ... args) {
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

		if (compatibleConstructors.size() > 1 && logger.isDebugEnabled()) {
			// TODO find + invoke the *most* *suitable* one - instead of logging this warning (and simply invoking the first).
			final String methodNameWithParameterTypes = createMethodNameWithParameterTypes(clazz.getSimpleName(), argTypes);
			final String msg = String.format("%s declares multiple constructors matching %s (or an equivalent using super-types of these parameter-types)!", clazz.getName(), methodNameWithParameterTypes);
//			throw new IllegalArgumentException(new NoSuchMethodException(msg));
			logger.warn("invokeConstructor: {}", msg);
		}

		return cast(invoke(compatibleConstructors.get(0), args));
	}

	public static <T> T invokeConstructor(final Class<T> clazz, Class<?>[] parameterTypes, final Object ... args) {
		final Constructor<T> constructor = getDeclaredConstructorOrFail(clazz, parameterTypes);
		return invoke(constructor, args);
	}

	public static <T> T invokeStatic(final Class<?> clazz, final String methodName, final Object ... args) {
		assertNotNull("clazz", clazz);
		assertNotNull("methodName", methodName);
		return invoke(clazz, (Object)null, methodName, args);
	}

	public static <T> T invokeStatic(final Class<?> clazz, final String methodName, Class<?>[] parameterTypes, final Object ... args) {
		final Method method = getDeclaredMethodOrFail(clazz, methodName, parameterTypes);
		return invoke((Object)null, method, args);
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

		if (compatibleMethods.size() > 1 && logger.isDebugEnabled()) {
			// TODO find + invoke the *most* *suitable* one - instead of logging this warning (and simply invoking the first).
			final String methodNameWithParameterTypes = createMethodNameWithParameterTypes(methodName, argTypes);
			final String msg = String.format("%s and its super-classes declare multiple methods matching %s (or an equivalent using super-types of these parameter-types)!", clazz.getName(), methodNameWithParameterTypes);
//			final Exception x = new NoSuchMethodException(msg);
			logger.warn("invoke: {}", msg);
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
		} catch (final IllegalArgumentException e) {
			throw e;
		} catch (final IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (final InvocationTargetException e) {
			final Throwable cause = e.getCause();
			if (cause instanceof RuntimeException)
				throw (RuntimeException) cause;
			else if (cause instanceof Error)
				throw (Error) cause;
			else
				throw new RuntimeException(cause);
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
			if (argTypes[i] != null
					&& ! parameterTypes[i].isAssignableFrom(argTypes[i])
					&& ! isSimpleTypeAssignmentPossible(parameterTypes[i], argTypes[i]))
				return false;
		}

		return true;
	}

	private static boolean isSimpleTypeAssignmentPossible(Class<?> parameterType, Class<?> argType) {
		if (parameterType == boolean.class && argType == Boolean.class)
			return true;

		if (parameterType == byte.class && argType == Byte.class)
			return true;

		if (parameterType == char.class && argType == Character.class)
			return true;

		if (parameterType == double.class && argType == Double.class)
			return true;

		if (parameterType == float.class && argType == Float.class)
			return true;

		if (parameterType == int.class && argType == Integer.class)
			return true;

		if (parameterType == long.class && argType == Long.class)
			return true;

		if (parameterType == short.class && argType == Short.class)
			return true;

		return false;
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

	public static <T> Constructor<T> getDeclaredConstructorOrFail(final Class<T> clazz, final Class<?>[] parameterTypes) {
		final Constructor<T> constructor;
		try {
			constructor = clazz.getDeclaredConstructor(parameterTypes);
		} catch (NoSuchMethodException | SecurityException e) {
			final String methodNameWithParameterTypes = createMethodNameWithParameterTypes(clazz.getName(), parameterTypes);
			throw new IllegalArgumentException(new NoSuchMethodException(String.format("%s does not declare the method %s!", clazz.getName(), methodNameWithParameterTypes)).initCause(e));
		}
		return constructor;
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
		if (parameterTypes == null)
			return name + "(...)";

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

	private static final Map<Class<?>, Reference<List<Field>>> class2AllDeclaredFields = Collections.synchronizedMap(
			new WeakHashMap<Class<?>, Reference<List<Field>>>());

	/**
	 * Gets all fields declared by the given {@code clazz} including its super-classes; starting from the most concrete sub-class
	 * (i.e. the given {@code clazz}).
	 * <p>
	 * Please note that the order of the fields declared by one single class is unspecified according to the Java specification.
	 * The only guaranteed order is that between fields declared by one class and fields declared by its super-classes: The result
	 * of this method begins with fields of the most concrete class, followed by its super-class's fields, followed by the
	 * fields of the super-class' super-class and so on.
	 * <p>
	 * For example, consider the following classes: {@code Roadster} <i>extends</i> {@code Car} <i>extends</i> {@code Vehicle}. This
	 * method would thus return all fields declared by all three classes, starting with the fields of {@code Roadster},
	 * followed by the fields of {@code Car} and finally followed by the fields of {@code Vehicle}.
	 *
	 * @param clazz the class whose fields to obtain.
	 * @return the list of fields declared by the given class and all its super-classes. Never <code>null</code>, but maybe
	 * empty.
	 */
	public static List<Field> getAllDeclaredFields(final Class<?> clazz) {
		assertNotNull("clazz", clazz);
		synchronized(clazz) {
			final Reference<List<Field>> resultRef = class2AllDeclaredFields.get(clazz);
			List<Field> result = resultRef == null ? null : resultRef.get();
			if (result == null) {
				result = new ArrayList<>();
				Class<?> c = clazz;
				while (c != null) {
					final Field[] declaredFields = c.getDeclaredFields();
					for (Field field : declaredFields)
						result.add(field);

					c = c.getSuperclass();
				}
				((ArrayList<?>)result).trimToSize();
				class2AllDeclaredFields.put(clazz, new WeakReference<List<Field>>(result));
			}
			return result;
		}
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

	public static <V> V getFieldValue(final Object object, final String fieldName) {
		assertNotNull("object", object);
		assertNotNull("fieldName", fieldName);

		// TODO pretty inefficient implementation - make better!

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
				@SuppressWarnings("unchecked")
				final V value = (V) field.get(object);
				return value;
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}

		throw new IllegalArgumentException(String.format("object's class %s does not have this field: %s", object.getClass(), fieldName));
	}

	/**
	 * Sets the {@code object}'s field &#8211; identified by {@code fieldName} &#8211; to the given {@code value}.
	 * <p>
	 * The {@code fieldName} can be simple (e.g. "firstName") or fully qualified (e.g. "co.codewizards.bla.Person.firstName").
	 * If it is simple, the most concrete sub-class' matching field is used.
	 *
	 * @param object the object whose field to manipulate. Must not be <code>null</code>.
	 * @param fieldName the simple or fully qualified field name (fully qualified means prefixed by the class name). Must not be <code>null</code>.
	 * @param value the value to be assigned. May be <code>null</code>.
	 */
	public static void setFieldValue(final Object object, final String fieldName, final Object value) {
		assertNotNull("object", object);
		assertNotNull("fieldName", fieldName);

		// TODO pretty inefficient implementation - make better!

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

		throw new IllegalArgumentException(String.format("object's class %s does not have this field: %s", object.getClass(), fieldName));
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

	/**
	 * Resolves the actual type arguments of a base-class declared in a concrete sub-class.
	 * <p>
	 * This is a convenience method delegating to {@link #resolveActualTypeArguments(Class, Class)}
	 * passing {@code concreteObject.getClass()} as {@code concreteClass}.
	 * @param baseClass the base class. Must not be <code>null</code>.
	 * @param concreteObject an instance of a sub-class of the generic {@code baseClass}.
	 * @return the resolved type arguments. Never <code>null</code> (empty array for a non-generic base-class).
	 */
	public static final <T> Type[] resolveActualTypeArguments(final Class<T> baseClass, final T concreteObject) {
		assertNotNull("baseClass", baseClass);
		assertNotNull("concreteObject", concreteObject);
		@SuppressWarnings("unchecked")
		final Class<? extends T> concreteClass = (Class<? extends T>) concreteObject.getClass();
		return resolveActualTypeArguments(baseClass, concreteClass);
	}

	/**
	 * Resolves the actual type arguments of a base-class declared in a concrete sub-class.
	 * <p>
	 * The length as well as the order of the resolved type arguments matches the declaration order
	 * in the base-class. If a type argument could successfully be resolved, it is usually an instance of
	 * {@link Class}. If it could not be resolved (because the sub-class does not specify the generic type info
	 * - directly or indirectly), it is an instance of {@link TypeVariable}.
	 * <p>
	 * A typical use-case is this:
	 * <pre>
	 * public abstract class MyBase&lt;A, B, C&gt; {
	 *   final Class&lt;A&gt; actualTypeArgumentA;
	 *   final Class&lt;B&gt; actualTypeArgumentB;
	 *   final Class&lt;C&gt; actualTypeArgumentC;
	 *
	 *   public MyBase() {
	 *     final Type[] actualTypeArguments = resolveActualTypeArguments(MyBase.class, this);
	 *
	 *     // The following assignments fail - of course -, if the concrete class lacks
	 *     // generic type info - like the example class "MyFail" below.
	 *     actualTypeArgumentA = (Class&lt;A&gt;) actualTypeArguments[0];
	 *     actualTypeArgumentB = (Class&lt;B&gt;) actualTypeArguments[1];
	 *     actualTypeArgumentC = (Class&lt;C&gt;) actualTypeArguments[2];
	 *   }
	 * }
	 *
	 * public class MyConcrete extends MyBase&lt;Long, Boolean, String&gt; {
	 * }
	 *
	 * public class MyFail extends MyBase {
	 * }
	 * </pre>
	 *
	 * @param baseClass the base class. Must not be <code>null</code>.
	 * @param concreteClass a sub-class of the generic {@code baseClass}.
	 * @return the resolved type arguments. Never <code>null</code> (empty array for a non-generic base-class).
	 */
	public static final <T> Type[] resolveActualTypeArguments(final Class<T> baseClass, final Class<? extends T> concreteClass) {
		return _resolveActualTypeArgs(baseClass, concreteClass);
	}

	private static final <T> Type[] _resolveActualTypeArgs(final Class<T> baseClass, final Class<? extends T> concreteClass, final Type... actualArgs) {
		assertNotNull("baseClass", baseClass);
		assertNotNull("concreteClass", concreteClass);
		assertNotNull("actualArgs", actualArgs);

	    if (actualArgs.length != 0 && actualArgs.length != concreteClass.getTypeParameters().length)
	    	throw new IllegalArgumentException("actualArgs.length != 0 && actualArgs.length != concreteClass.typeParameters.length");

	    final Type[] _actualArgs = actualArgs.length == 0 ? concreteClass.getTypeParameters() : actualArgs;

	    // map type parameters into the actual types
	    Map<String, Type> typeVariables = new HashMap<String, Type>();
	    for (int i = 0; i < _actualArgs.length; i++) {
	        TypeVariable<?> typeVariable = concreteClass.getTypeParameters()[i];
	        typeVariables.put(typeVariable.getName(), _actualArgs[i]);
	    }

	    // Find direct ancestors (superclass, interfaces)
	    List<Type> ancestors = new LinkedList<Type>();
	    if (concreteClass.getGenericSuperclass() != null) {
	        ancestors.add(concreteClass.getGenericSuperclass());
	    }
	    for (Type t : concreteClass.getGenericInterfaces()) {
	        ancestors.add(t);
	    }

	    // Recurse into ancestors (superclass, interfaces)
	    for (Type type : ancestors) {
	        if (type instanceof Class<?>) {
	            // ancestor is non-parameterized. Recurse only if it matches the base class.
	            Class<?> ancestorClass = (Class<?>) type;
	            if (baseClass.isAssignableFrom(ancestorClass)) {
	                Type[] result = _resolveActualTypeArgs(baseClass, (Class<? extends T>) ancestorClass);
	                if (result != null) {
	                    return result;
	                }
	            }
	        }
	        if (type instanceof ParameterizedType) {
	            // ancestor is parameterized. Recurse only if the raw type matches the base class.
	            ParameterizedType parameterizedType = (ParameterizedType) type;
	            Type rawType = parameterizedType.getRawType();
	            if (rawType instanceof Class<?>) {
	                Class<?> rawTypeClass = (Class<?>) rawType;
	                if (baseClass.isAssignableFrom(rawTypeClass)) {

	                    // loop through all type arguments and replace type variables with the actually known types
	                    List<Type> resolvedTypes = new LinkedList<Type>();
	                    for (Type t : parameterizedType.getActualTypeArguments()) {
	                        if (t instanceof TypeVariable<?>) {
	                            Type resolvedType = typeVariables.get(((TypeVariable<?>) t).getName());
	                            resolvedTypes.add(resolvedType != null ? resolvedType : t);
	                        } else {
	                            resolvedTypes.add(t);
	                        }
	                    }

	                    Type[] result = _resolveActualTypeArgs(baseClass, (Class<? extends T>) rawTypeClass, resolvedTypes.toArray(new Type[] {}));
	                    if (result != null) {
	                        return result;
	                    }
	                }
	            }
	        }
	    }

	    // we have a result if we reached the base class.
	    return concreteClass.equals(baseClass) ? _actualArgs : null;
	}
}
