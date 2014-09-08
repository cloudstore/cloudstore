package co.codewizards.cloudstore.core.objectfactory;

public class ObjectFactoryUtil {
	public static <T> T create(final Class<T> clazz) {
		return ObjectFactory.getInstance().create(clazz);
	}

	public static <T> T create(final Class<T> clazz, final Object ... parameters) {
		return ObjectFactory.getInstance().create(clazz, (Class<?>[]) null, parameters);
	}

	public <T> T create(final Class<T> clazz, final Class<?>[] parameterTypes, final Object ... parameters) {
		return ObjectFactory.getInstance().create(clazz, parameterTypes, parameters);
	}
}
