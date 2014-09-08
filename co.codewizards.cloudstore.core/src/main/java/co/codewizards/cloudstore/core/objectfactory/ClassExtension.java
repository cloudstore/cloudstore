package co.codewizards.cloudstore.core.objectfactory;


public interface ClassExtension<T> {

	int getPriority();

	Class<T> getBaseClass();

	Class<? extends T> getExtendingClass();

}
