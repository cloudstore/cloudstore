package co.codewizards.cloudstore.core.util;

import java.util.Arrays;

/**
 * @author Marco
 * @author Sebastian Schefczyk
 */
public final class Util {
	private Util() { }

	/**
	 * Get the user name of the user who is currently authenticated at the operating system.
	 * This method simply calls <code>System.getProperty("user.name");</code>.
	 *
	 * @return the user name of the current operating system user.
	 */
	public static final String getUserName()
	{
		return System.getProperty("user.name"); //$NON-NLS-1$
	}

	public static final boolean equal(final Object one, final Object two) {
		return one == null ? two == null : one.equals(two);
	}

	public static final boolean equal(final boolean one, final boolean two) {
		return one == two;
	}

	public static final boolean equal(final byte one, final byte two) {
		return one == two;
	}

	public static final boolean equal(final short one, final short two) {
		return one == two;
	}

	public static final boolean equal(final char one, final char two) {
		return one == two;
	}

	public static final boolean equal(final int one, final int two) {
		return one == two;
	}

	public static final boolean equal(final long one, final long two) {
		return one == two;
	}


	public static final boolean equal(final Object[] one, final Object[] two) {
		return Arrays.equals(one, two);
	}

	public static final boolean equal(final byte[] one, final byte[] two) {
		return Arrays.equals(one, two);
	}

	public static final boolean equal(final boolean[] one, final boolean[] two) {
		return Arrays.equals(one, two);
	}

	public static final boolean equal(final short[] one, final short[] two) {
		return Arrays.equals(one, two);
	}

	public static final boolean equal(final char[] one, final char[] two) {
		return Arrays.equals(one, two);
	}

	public static final boolean equal(final int[] one, final int[] two) {
		return Arrays.equals(one, two);
	}

	public static final boolean equal(final long[] one, final long[] two) {
		return Arrays.equals(one, two);
	}

	@SuppressWarnings("unchecked")
	public static <T> T cast(final Object o) {
		return (T) o;
	}

	/**
	 * Gets the {@code String}-representation of the given {@code object} as it is constructed by the base-implementation
	 * {@link Object#toString()}.
	 * <p>
	 * This method does not invoke the object's {@code toString()} method, hence it makes
	 * no difference, if the object's class has an overridden version of the {@code toString()} method!
	 * <p>
	 * If the given {@code object} is <code>null</code>, the result is {@code "null"} (just like the result of
	 * {@link String#valueOf(Object)}).
	 * <p>
	 * If the given {@code object} is not <code>null</code>, the result consists of the fully qualified class-name followed
	 * by '@' and the object's hex-encoded {@linkplain System#identityHashCode(Object) identity-hash-code}. For example:
	 * {@code "my.package.Car@47c624e2"}.
	 *
	 * @param object the object for which to obtain the {@code String}-representation. May be <code>null</code>.
	 * @return the {@code String}-representation. Never <code>null</code>.
	 */
	public static String toIdentityString(final Object object) {
		if (object == null)
			return String.valueOf(object); // result: "null"

		return object.getClass().getName() + '@' + Integer.toHexString(System.identityHashCode(object));
	}

	public static void main(String[] args) {
		System.out.println(toIdentityString(new Object()));
	}

	/**
	 * Does really nothing.
	 * <p>
	 * This method should be used in the catch of a try-catch-block, if there's really no action needed.
	 */
	public static final void doNothing() { }
}
