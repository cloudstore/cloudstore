package co.codewizards.cloudstore.core.objectfactory;

import static java.util.Objects.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * Factory for objects.
 * <p>
 * Instead of invoking {@code new MySomething()}, devs can import {@link ObjectFactoryUtil}<code>.*</code>
 * statically and then invoke {@link ObjectFactoryUtil#createObject(Class) createObject(MySomething.class)}.
 * Thus allowing downstream projects to extend the system by providing a replacement-class. For example, if the
 * replacement-class {@code MyOther} was registered for {@code MySomething}, the method
 * {@code createObject(MySomething.class)} would return an instance of {@code MyOther} instead of
 * {@code MySomething}.
 * <p>
 * However, it is urgently recommended <i>not</i> to use this approach, whenever it is possible to use a better solution,
 * preferably a well-defined service (=&gt; {@link ServiceLoader}). There are situations, e.g. data-model-classes
 * (a.k.a. entities), where services are not possible and the {@code ObjectFactory} is the perfect solution.
 * <p>
 * In order to register a sub-class as replacement for a certain base-class, implementors have to provide a
 * {@link ClassExtension} and register it using the {@link ServiceLoader}-mechanism.
 * <p>
 * Important: You should usually <i>not</i> need to access this class directly! Use {@link ObjectFactoryUtil} instead
 * (statically import its methods).
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public class ObjectFactory {

	private final Map<Class<?>, ClassExtension<?>> baseClass2ClassExtension;

	private static final Class<?>[] EMPTY_CLASS_ARRAY = new Class[0];

	private static final class Holder {
		public static final ObjectFactory instance = new ObjectFactory();
	}

	/**
	 * Gets the singleton instance of this {@code ObjectFactory}.
	 * <p>
	 * <b>Important:</b> You should normally <i>not</i> invoke this method directly, but use {@link ObjectFactoryUtil}
	 * instead.
	 * @return the {@code ObjectFactory} instance; never <code>null</code>.
	 */
	public static ObjectFactory getInstance() {
		return Holder.instance;
	}

	protected ObjectFactory() {
		final Map<Class<?>, ClassExtension<?>> baseClass2ClassExtension = new HashMap<Class<?>, ClassExtension<?>>();
		for (final ClassExtension<?> classExtension : ServiceLoader.load(ClassExtension.class)) {
			final ClassExtension<?> old = baseClass2ClassExtension.get(classExtension.getBaseClass());
			if (old == null || old.getPriority() < classExtension.getPriority())
				baseClass2ClassExtension.put(classExtension.getBaseClass(), classExtension);
			else if (old.getPriority() == classExtension.getPriority())
				throw new IllegalStateException("Multiple ClassExtensions registered on the base-class %s with the same priority!");
		}
		this.baseClass2ClassExtension = Collections.unmodifiableMap(baseClass2ClassExtension);
	}

	public <T> Class<? extends T> getExtendingClass(final Class<T> clazz) {
		Class<? extends T> c = clazz;
		ClassExtension<? extends T> classExtension;
		while (null != (classExtension = getClassExtension(c))) {
			c = classExtension.getExtendingClass();
		}
		return c;
	}

	@SuppressWarnings("unchecked")
	public <T> ClassExtension<T> getClassExtension(final Class<T> clazz) {
		return (ClassExtension<T>) baseClass2ClassExtension.get(clazz);
	}

	public <T> T createObject(final Class<T> clazz) {
		return createObject(clazz, (Class<?>[]) null, (Object[]) null);
	}

	public <T> T createObject(final Class<T> clazz, final Object ... parameters) {
		return createObject(clazz, (Class<?>[]) null, parameters);
	}

	public <T> T createObject(final Class<T> clazz, Class<?>[] parameterTypes, final Object ... parameters) {
		requireNonNull(clazz, "clazz");
		if (parameterTypes != null && parameters != null) {
			if (parameterTypes.length != parameters.length)
				throw new IllegalArgumentException(String.format(
						"parameterTypes.length != parameters.length :: %s != %s", parameterTypes.length, parameters.length));
		}

		if (parameterTypes == null && (parameters == null || parameters.length == 0))
			parameterTypes = EMPTY_CLASS_ARRAY;

		final Class<? extends T> c = getExtendingClass(clazz);

		Constructor<? extends T> constructor;
		if (parameterTypes == null && parameters != null)
			constructor = getMatchingConstructor(c, parameters);
		else {
			for (int i = 0; i < parameterTypes.length; ++i) {
				if (parameterTypes[i] == null)
					throw new IllegalArgumentException(String.format("parameterTypes[%s] == null", i));
			}
			try {
				constructor = c.getDeclaredConstructor(parameterTypes);
			} catch (final NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
		}

		constructor.setAccessible(true);

		try {
			final T instance = constructor.newInstance(parameters);
			return instance;
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	private <T> Constructor<T> getMatchingConstructor(final Class<T> clazz, final Object[] parameters) {
		requireNonNull(clazz, "clazz");
		requireNonNull(parameters, "parameters");
		final Constructor<?>[] constructors = clazz.getDeclaredConstructors();
		final List<Constructor<T>> constructorsWithSameNumberOfArguments = new LinkedList<Constructor<T>>();
		for (final Constructor<?> constructor : constructors) {
			if (constructor.getParameterTypes().length == parameters.length) {
				@SuppressWarnings("unchecked")
				final
				Constructor<T> con = (Constructor<T>) constructor;
				constructorsWithSameNumberOfArguments.add(con);
			}
		}

		if (constructorsWithSameNumberOfArguments.isEmpty())
			throw new RuntimeException(new NoSuchMethodException(String.format("The class %s does not have any constructor with %s arguments.", clazz.getName(), parameters.length)));

		if (constructorsWithSameNumberOfArguments.size() == 1)
			return constructorsWithSameNumberOfArguments.get(0);

		throw new UnsupportedOperationException(String.format("The class %s has multiple constructors with %s arguments. This is NOT YET SUPPORTED!", clazz.getName(), parameters.length));
	}
}
