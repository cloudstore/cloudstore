package co.codewizards.cloudstore.core.util;

import static java.util.Objects.*;

import java.util.Collection;

/**
 * @author Sebastian Schefczyk
 *
 */
public final class AssertUtil {

	private AssertUtil() { }

	public static final <T> T assertNotNull(final T object, final String name, final String additionalInfoTemplate, final Object ... additionalInfoArgs) {
		if (additionalInfoTemplate == null)
			return requireNonNull(object, name);

		if (object == null)
			throw new IllegalArgumentException(String.format("%s == null :: ", name) + String.format(additionalInfoTemplate, additionalInfoArgs));

		return object;
	}

	public static final <T> T[] assertNotNullAndNoNullElement(final T[] array, final String name) {
		requireNonNull(array, name);
		for (int i = 0; i < array.length; i++) {
			if (array[i] == null)
				throw new IllegalArgumentException(String.format("%s[%s] == null", name, i));
		}
		return array;
	}

	public static final <E, T extends Collection<E>> T assertNotNullAndNoNullElement(final T collection, final String name) {
		requireNonNull(collection, name);
		int i = -1;
		for (final E element : collection) {
			++i;
			if (element == null)
				throw new IllegalArgumentException(String.format("%s[%s] == null", name, i));
		}
		return collection;
	}

	public static final <E, T extends Collection<E>> T assertNotEmpty(final T collection, final String name) {
		requireNonNull(collection, name);
		if (collection.isEmpty())
			throw new IllegalArgumentException(String.format("%s is empty", name));

		return collection;
	}
}
