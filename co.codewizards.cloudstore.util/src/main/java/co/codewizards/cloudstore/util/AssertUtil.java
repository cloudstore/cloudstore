package co.codewizards.cloudstore.util;

import java.util.Collection;

/**
 * @author Sebastian Schefczyk
 *
 */
public final class AssertUtil {

	private AssertUtil() { }

	public static final <T> T assertNotNull(final String name, final T object) {
		if (object == null)
			throw new IllegalArgumentException(String.format("%s == null", name));

		return object;
	}

	public static final <T> T[] assertNotNullAndNoNullElement(final String name, final T[] array) {
		assertNotNull(name, array);
		for (int i = 0; i < array.length; i++) {
			if (array[i] == null)
				throw new IllegalArgumentException(String.format("%s[%s] == null", name, i));
		}
		return array;
	}

	public static final <E, T extends Collection<E>> T assertNotNullAndNoNullElement(final String name, final T collection) {
		assertNotNull(name, collection);
		int i = -1;
		for (final E element : collection) {
			++i;
			if (element == null)
				throw new IllegalArgumentException(String.format("%s[%s] == null", name, i));
		}
		return collection;
	}

}
