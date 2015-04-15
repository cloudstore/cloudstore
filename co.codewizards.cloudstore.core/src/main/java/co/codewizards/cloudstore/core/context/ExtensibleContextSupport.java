package co.codewizards.cloudstore.core.context;

import static co.codewizards.cloudstore.core.util.AssertUtil.assertNotNull;

import java.util.HashMap;
import java.util.Map;

public class ExtensibleContextSupport implements ExtensibleContext {

	private final Map<Class<?>, Object> contextClass2ContextObject = new HashMap<>();

	@Override
	public void setContextObject(final Object object) {
		assertNotNull("object", object);
		Class<?> clazz = object.getClass();
		if (clazz == Object.class)
			throw new IllegalArgumentException("object is of type java.lang.Object! Must be a sub-class!");

		while (clazz != Object.class) {
			registerContextObject(clazz, object);
			clazz = clazz.getSuperclass();
		}
	}

	private void registerContextObject(final Class<?> clazz, final Object object) {
		contextClass2ContextObject.put(clazz, object);

		final Class<?>[] interfaces = clazz.getInterfaces();
		for (final Class<?> iface : interfaces)
			registerContextObject(iface, object);
	}

	@Override
	public <T> T getContextObject(final Class<T> clazz) {
		assertNotNull("clazz", clazz);
		return clazz.cast(contextClass2ContextObject.get(clazz));
	}

	@Override
	public void removeContextObject(Class<?> clazz) {
		assertNotNull("clazz", clazz);
		contextClass2ContextObject.remove(clazz);
	}
}
