package co.codewizards.cloudstore.core.objectfactory;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Abstract base-class for the much easier implementation of a {@link ClassExtension}.
 * <p>
 * It is highly recommended not to implement the interface {@code ClassExtension} directly. Instead,
 * implementors should always extend this abstract base-class.
 * <p>
 * In most cases, an implementation of a {@code ClassExtension} looks simply like this example:
 * <pre>
 *  public class SsSymlinkClassExtension extends AbstractClassExtension&lt;Symlink, SsSymlink&gt; {
 *  }
 * </pre>
 * <p>
 * It is recommended to use the naming scheme "${extendingClassName}" + "ClassExtension" as shown in
 * the example above.
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 *
 * @param <T> the type of the base-class. Automatically assigned to the property {@link #getBaseClass() baseClass}.
 * @param <E> the type of the extending class. Automatically assigned to the property {@link #getExtendingClass() extendingClass}.
 */
public abstract class AbstractClassExtension<T, E extends T> implements ClassExtension<T> {

	private final Class<T> baseClass;

	private final Class<E> extendingClass;

	public AbstractClassExtension() {
		final ParameterizedType superclass = (ParameterizedType) getClass().getGenericSuperclass();
		final Type[] actualTypeArguments = superclass.getActualTypeArguments();
		if (actualTypeArguments == null || actualTypeArguments.length < 2)
			throw new IllegalStateException("Subclass " + getClass().getName() + " has no generic type argument!");

		@SuppressWarnings("unchecked")
		final Class<T> c1 = (Class<T>) actualTypeArguments[0];
		this.baseClass = c1;
		if (this.baseClass == null)
			throw new IllegalStateException("Subclass " + getClass().getName() + " has no generic type argument!");

		@SuppressWarnings("unchecked")
		final Class<E> c2 = (Class<E>) actualTypeArguments[1];
		this.extendingClass = c2;
		if (this.extendingClass == null)
			throw new IllegalStateException("Subclass " + getClass().getName() + " has no generic type argument!");
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * The default implementation in {@link AbstractClassExtension} returns 0. Override and return
	 * either a negative value, if you want to provide a fallback-implementation (that is likely to be
	 * replaced by a different {@code ClassExtension}) or a positive value in order to override
	 * another {@code ClassExtension} (make sure your priority is greater than the other extension's priority).
	 */
	@Override
	public int getPriority() {
		return 0;
	}

	@Override
	public Class<T> getBaseClass() {
		return baseClass;
	}

	@Override
	public Class<E> getExtendingClass() {
		return extendingClass;
	}

}
