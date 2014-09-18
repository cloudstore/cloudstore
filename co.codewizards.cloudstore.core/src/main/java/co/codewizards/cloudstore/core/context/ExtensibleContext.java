package co.codewizards.cloudstore.core.context;

public interface ExtensibleContext {

	void setContextObject(Object object);

	<T> T getContextObject(Class<T> clazz);

}
