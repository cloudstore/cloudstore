package co.codewizards.cloudstore.core.io;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Utility class for working with {@link IInputStream} and {@link IOutputStream}.
 * <p>
 * Most importantly, the methods here are used for conversions:
 * <ul>
 * <li>{@link InputStream} &lt;=&gt; {@link IInputStream}
 * <li>{@link OutputStream} &lt;=&gt; {@link IOutputStream}
 * </ul>
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public final class StreamUtil {
	private StreamUtil() {
	}

	/**
	 * Casts an {@link InputStream} as {@link IInputStream}, if possible; or
	 * converts it by instantiating a new bridge instance, if a simple Java cast is not possible.
	 * <p>
	 * Note that the bridge object (if its instantiation was needed) both subclasses {@link InputStream}
	 * and implements {@link IInputStream}. Thus subsequent <code>castStream(...)</code> invocations on
	 * the result object always are simple Java casts.
	 *
	 * @param in the {@link InputStream} to be converted (by Java cast or bridge object). May be <code>null</code>.
	 * @return either the same instance as {@code in}, if a simple Java cast was possible; or a new
	 * bridge object implementing the desired interface and delegating to the underlying object.
	 * May be <code>null</code> (only if the input argument is <code>null</code>).
	 */
	public static IInputStream castStream(final InputStream in) {
		if (in instanceof IInputStream)
			return (IInputStream) in;

		if (in == null)
			return null;

		return new InStream(in);
	}

	/**
	 * Casts an {@link IInputStream} as {@link InputStream}, if possible; or
	 * converts it by instantiating a new bridge instance, if a simple Java cast is not possible.
	 * <p>
	 * Note that the bridge object (if its instantiation was needed) both subclasses {@link InputStream}
	 * and implements {@link IInputStream}. Thus subsequent <code>castStream(...)</code> invocations on
	 * the result object always are simple Java casts.
	 *
	 * @param in the {@link IInputStream} to be converted (by Java cast or bridge object). May be <code>null</code>.
	 * @return either the same instance as {@code in}, if a simple Java cast was possible; or a new
	 * bridge object extending {@link InputStream} and delegating to the underlying object.
	 * May be <code>null</code> (only if the input argument is <code>null</code>).
	 */
	public static InputStream castStream(final IInputStream in) {
		if (in instanceof InputStream)
			return (InputStream) in;

		if (in == null)
			return null;

		return new InStream.InverseInStream(in);
	}

	/**
	 * Casts an {@link OutputStream} as {@link IOutputStream}, if possible; or
	 * converts it by instantiating a new bridge instance, if a simple Java cast is not possible.
	 * <p>
	 * Note that the bridge object (if its instantiation was needed) both subclasses {@link OutputStream}
	 * and implements {@link IOutputStream}. Thus subsequent <code>castStream(...)</code> invocations on
	 * the result object always are simple Java casts.
	 *
	 * @param out the {@link OutputStream} to be converted (by Java cast or bridge object). May be <code>null</code>.
	 * @return either the same instance as {@code out}, if a simple Java cast was possible; or a new
	 * bridge object implementing the desired interface and delegating to the underlying object.
	 * May be <code>null</code> (only if the input argument is <code>null</code>).
	 */
	public static IOutputStream castStream(final OutputStream out) {
		if (out instanceof IOutputStream)
			return (IOutputStream) out;

		if (out == null)
			return null;

		return new OutStream(out);
	}

	/**
	 * Casts an {@link IOutputStream} as {@link OutputStream}, if possible; or
	 * converts it by instantiating a new bridge instance, if a simple Java cast is not possible.
	 * <p>
	 * Note that the bridge object (if its instantiation was needed) both subclasses {@link OutputStream}
	 * and implements {@link IOutputStream}. Thus subsequent <code>castStream(...)</code> invocations on
	 * the result object always are simple Java casts.
	 *
	 * @param out the {@link IOutputStream} to be converted (by Java cast or bridge object). May be <code>null</code>.
	 * @return either the same instance as {@code out}, if a simple Java cast was possible; or a new
	 * bridge object extending {@link OutputStream} and delegating to the underlying object.
	 * May be <code>null</code> (only if the input argument is <code>null</code>).
	 */
	public static OutputStream castStream(final IOutputStream out) {
		if (out instanceof OutputStream)
			return (OutputStream) out;

		if (out == null)
			return null;

		return new OutStream.InverseOutStream(out);
	}
}
