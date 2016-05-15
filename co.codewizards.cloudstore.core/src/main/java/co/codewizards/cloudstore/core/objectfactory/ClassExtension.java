package co.codewizards.cloudstore.core.objectfactory;

import java.util.ServiceLoader;

/**
 * A {@code ClassExtension} declares that a sub-class must be instantiated instead of a certain base-class.
 * <p>
 * In order to register a sub-class as replacement for a certain base-class, implementors have to provide a
 * {@code ClassExtension} and register it using the {@link ServiceLoader}-mechanism. This means, they must place a file
 * named {@code co.codewizards.cloudstore.core.objectfactory.ClassExtension} into {@code src/main/resources/META-INF/services/}
 * in their extension-project and enlist their {@code ClassExtension}-implementation there.
 * <p>
 * <b>Important:</b> It is recommended <i>not</i> to directly implement this interface, but to sub-class {@link AbstractClassExtension}
 * instead.
 * <p>
 * <b>Important 2:</b> If a certain base-class should be replaceable using this mechanism, <b>all</b> occurrences
 * of {@code new MyBaseClass(...)} in the entire code-base must be replaced by
 * {@link ObjectFactoryUtil#createObject(Class) createObject(MyBaseClass.class, ...)}.
 * <p>
 * <b>Important 3:</b> It is urgently recommended <i>not</i> to use this approach, whenever it is possible to use a better solution,
 * preferably a well-defined service (=&gt; {@link ServiceLoader}). There are situations, though, e.g. data-model-classes
 * (a.k.a. entities), where services are not possible and {@link ObjectFactoryUtil} + {@code ClassExtension} is the perfect solution.
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 *
 * @param <T> the type of the base-class to be extended (i.e. replaced in all instantiations) by a certain sub-class.
 */
public interface ClassExtension<T> {

	/**
	 * Gets the priority of this extension.
	 * <p>
	 * If there are multiple {@code ClassExtension}-implementations for the same {@link #getBaseClass() baseClass}, the
	 * extension with the highest priority (i.e the greatest number returned here) is chosen.
	 * @return the priority of this extension. Might be negative, 0 or positive.
	 */
	int getPriority();

	/**
	 * Gets the base-class to be extended by instantiating a sub-class instead.
	 * @return the base-class to be replaced in all instantiations. Never <code>null</code>.
	 */
	Class<T> getBaseClass();

	/**
	 * Gets the sub-class extending the {@link #getBaseClass() baseClass} to be instantiated whenever
	 * a new instance of the base-class is requested.
	 * @return the sub-class to be instantiated instead of the {@link #getBaseClass() baseClass}. Never <code>null</code>.
	 */
	Class<? extends T> getExtendingClass();
}
