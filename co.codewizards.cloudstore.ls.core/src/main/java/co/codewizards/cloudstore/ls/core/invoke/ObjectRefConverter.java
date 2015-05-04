package co.codewizards.cloudstore.ls.core.invoke;

public interface ObjectRefConverter {

	Object convertToObjectRefIfNeeded(Object object);

	Object convertFromObjectRefIfNeeded(Object object);

}
