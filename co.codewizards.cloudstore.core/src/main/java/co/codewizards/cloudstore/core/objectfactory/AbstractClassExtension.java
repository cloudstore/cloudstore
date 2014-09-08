package co.codewizards.cloudstore.core.objectfactory;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public abstract class AbstractClassExtension<T, E extends T> implements ClassExtension<T> {

	private final Class<T> baseClass;

	private final Class<E> extendingClass;

	public AbstractClassExtension() {
		final ParameterizedType superclass = (ParameterizedType) getClass().getGenericSuperclass();
		final Type[] actualTypeArguments = superclass.getActualTypeArguments();
		if (actualTypeArguments == null || actualTypeArguments.length < 1)
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
