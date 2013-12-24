package co.codewizards.cloudstore.shared.util;

public class ObjectUtil {
	public static <T> T assertNotNull(String name, T object) {
		if (object == null)
			throw new IllegalArgumentException(String.format("%s == null", name));

		return object;
	}

	public static boolean equals(Object one, Object two) {
		return one == null ? two == null : one.equals(two);
	}
}
