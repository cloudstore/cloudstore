package co.codewizards.cloudstore.core.objectfactory;

public abstract class AbstractClassExtension<T> implements ClassExtension<T> {

	@Override
	public int getPriority() {
		return 0;
	}

}
