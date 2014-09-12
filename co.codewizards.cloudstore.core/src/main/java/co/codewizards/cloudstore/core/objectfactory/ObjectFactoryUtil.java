package co.codewizards.cloudstore.core.objectfactory;

public class ObjectFactoryUtil {
	protected ObjectFactoryUtil() { }

	public static <T> T createObject(final Class<T> clazz) {
		return ObjectFactory.getInstance().create(clazz);
	}

	public static <T> T createObject(final Class<T> clazz, final Object ... parameters) {
		return ObjectFactory.getInstance().create(clazz, (Class<?>[]) null, parameters);
	}

	public static <T> T createObject(final Class<T> clazz, final Class<?>[] parameterTypes, final Object ... parameters) {
		return ObjectFactory.getInstance().create(clazz, parameterTypes, parameters);
	}

	public static <T> Class<? extends T> getExtendingClass(final Class<T> clazz) {
		return ObjectFactory.getInstance().getExtendingClass(clazz);
	}
}
