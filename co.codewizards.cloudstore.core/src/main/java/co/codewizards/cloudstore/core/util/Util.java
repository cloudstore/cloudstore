package co.codewizards.cloudstore.core.util;

import java.util.Arrays;
import java.util.Collection;

import co.codewizards.cloudstore.util.AssertUtil;

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

	public static final <T> T assertNotNull(final String name, final T object) {
		return AssertUtil.assertNotNull(name, object);
	}

	public static final <T> T[] assertNotNullAndNoNullElement(final String name, final T[] array) {
		return AssertUtil.assertNotNullAndNoNullElement(name, array);
	}

	public static final <E, T extends Collection<E>> T assertNotNullAndNoNullElement(final String name, final T collection) {
		return AssertUtil.assertNotNullAndNoNullElement(name, collection);
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


	/**
	 * Does really nothing.
	 * <p>
	 * This method should be used in the catch of a try-catch-block, if there's really no action needed.
	 */
	public static final void doNothing() { }
}
