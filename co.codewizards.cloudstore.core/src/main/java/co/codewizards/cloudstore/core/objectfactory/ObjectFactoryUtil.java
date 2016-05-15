package co.codewizards.cloudstore.core.objectfactory;

import java.util.ServiceLoader;

/**
 * Simple API to use the {@link ObjectFactory}.
 * <p>
 * Devs should add the following import:
 * <pre>
 * import static co.codewizards.cloudstore.core.objectfactory.ObjectFactoryUtil.*;
 * </pre>
 * They can then invoke one of the {@code createObject(...)} methods simply without any prefix.
 * <p>
 * In order to register a sub-class as replacement for a certain base-class, implementors have to provide a
 * {@link ClassExtension} and register it using the {@link ServiceLoader}-mechanism.
 * <p>
 * <b>Important:</b> If a certain base-class should be replaceable using this mechanism, <b>all</b> occurrences
 * of {@code new MyBaseClass(...)} in the entire code-base must be replaced by {@code createObject(MyBaseClass.class, ...)}.
 * <p>
 * <b>Important 2:</b> It is urgently recommended <i>not</i> to use this approach, whenever it is possible to use a better solution,
 * preferably a well-defined service (=&gt; {@link ServiceLoader}). There are situations, though, e.g. data-model-classes
 * (a.k.a. entities), where services are not possible and {@code ObjectFactoryUtil} + {@code ClassExtension} is the perfect solution.
 * <p>
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public class ObjectFactoryUtil {
	protected ObjectFactoryUtil() { }

	/**
	 * Create an instance of the given class or a sub-class registered as replacement.
	 * <p>
	 * If at least one {@link ClassExtension} is registered for the given class, the {@code ClassExtension}
	 * with the highest {@link ClassExtension#getPriority() priority} is chosen and its
	 * {@link ClassExtension#getExtendingClass() extendingClass} instantiated instead of the base-class passed as {@code clazz}.
	 * <p>
	 * This method always invokes the default constructor (without any parameters).
	 * @param clazz the base-class to be instantiated. Must not be <code>null</code>.
	 * @return the object instantiated either from the given base-class or a sub-class registered via {@link ClassExtension}.
	 * Never <code>null</code>.
	 * @see #createObject(Class, Class[], Object...)
	 */
	public static <T> T createObject(final Class<T> clazz) {
		return ObjectFactory.getInstance().createObject(clazz);
	}

	/**
	 * Create an instance of the given class or a sub-class registered as replacement.
	 * <p>
	 * If at least one {@link ClassExtension} is registered for the given class, the {@code ClassExtension}
	 * with the highest {@link ClassExtension#getPriority() priority} is chosen and its
	 * {@link ClassExtension#getExtendingClass() extendingClass} instantiated instead of the base-class passed as {@code clazz}.
	 * <p>
	 * This method tries to automatically find the best constructor matching the given parameters.
	 * If multiple constructors might match and they are not semantically the same (only doing some implicit conversions),
	 * you should instead use {@link #createObject(Class, Class[], Object...)} and pass the specific parameter types.
	 * @param clazz the base-class to be instantiated. Must not be <code>null</code>.
	 * @param parameters the parameters to be passed to the constructor.
	 * @return the object instantiated either from the given base-class or a sub-class registered via {@link ClassExtension}.
	 * Never <code>null</code>.
	 * @see #createObject(Class, Class[], Object...)
	 */
	public static <T> T createObject(final Class<T> clazz, final Object ... parameters) {
		return ObjectFactory.getInstance().createObject(clazz, (Class<?>[]) null, parameters);
	}

	/**
	 * Create an instance of the given class or a sub-class registered as replacement.
	 * <p>
	 * If at least one {@link ClassExtension} is registered for the given class, the {@code ClassExtension}
	 * with the highest {@link ClassExtension#getPriority() priority} is chosen and its
	 * {@link ClassExtension#getExtendingClass() extendingClass} instantiated instead of the base-class passed as {@code clazz}.
	 * <p>
	 * This method invokes the constructor matching exactly the given parameter-types. If {@code parameterTypes} is
	 * <code>null</code>, a matching constructor is searched automatically based on the concrete {@code parameters}.
	 *
	 * @param clazz the base-class to be instantiated. Must not be <code>null</code>.
	 * @param parameterTypes either <code>null</code> or the exact argument-types of the constructor to be invoked.
	 * @param parameters the parameters to be passed to the constructor.
	 * @return the object instantiated either from the given base-class or a sub-class registered via {@link ClassExtension}.
	 * Never <code>null</code>.
	 */
	public static <T> T createObject(final Class<T> clazz, final Class<?>[] parameterTypes, final Object ... parameters) {
		return ObjectFactory.getInstance().createObject(clazz, parameterTypes, parameters);
	}

	public static <T> Class<? extends T> getExtendingClass(final Class<T> clazz) {
		return ObjectFactory.getInstance().getExtendingClass(clazz);
	}
}
