package co.codewizards.cloudstore.core.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

import co.codewizards.cloudstore.core.progress.NullProgressMonitor;
import co.codewizards.cloudstore.core.progress.ProgressMonitor;

public final class HashUtil {

	/**
	 * Specifies usage of the MD5 algorithm in {@link #hash(String, InputStream)}.
	 */
	public static final String HASH_ALGORITHM_MD5 = "MD5";
	/**
	 * Specifies usage of the SHA algorithm in {@link #hash(String, InputStream)}.
	 */
	public static final String HASH_ALGORITHM_SHA = "SHA";

	private HashUtil() { }

	public static String encodeHexStr(final byte[] buf)
	{
		return encodeHexStr(buf, 0, buf.length);
	}

	/**
	 * Encode a byte array into a human readable hex string. For each byte,
	 * two hex digits are produced. They are concatenated without any separators.
	 *
	 * @param buf The byte array to translate into human readable text.
	 * @param pos The start position (0-based).
	 * @param len The number of bytes that shall be processed beginning at the position specified by <code>pos</code>.
	 * @return a human readable string like "fa3d70" for a byte array with 3 bytes and these values.
	 * @see #encodeHexStr(byte[])
	 * @see #decodeHexStr(String)
	 */
	public static String encodeHexStr(final byte[] buf, int pos, int len)
	{
		 final StringBuilder hex = new StringBuilder();
		 while (len-- > 0) {
				final byte ch = buf[pos++];
				int d = (ch >> 4) & 0xf;
				hex.append((char)(d >= 10 ? 'a' - 10 + d : '0' + d));
				d = ch & 0xf;
				hex.append((char)(d >= 10 ? 'a' - 10 + d : '0' + d));
		 }
		 return hex.toString();
	}

	/**
	 * Decode a string containing two hex digits for each byte.
	 * @param hex The hex encoded string
	 * @return The byte array represented by the given hex string
	 * @see #encodeHexStr(byte[])
	 * @see #encodeHexStr(byte[], int, int)
	 */
	public static byte[] decodeHexStr(final String hex)
	{
		if (hex.length() % 2 != 0)
			throw new IllegalArgumentException("The hex string must have an even number of characters!");

		final byte[] res = new byte[hex.length() / 2];

		int m = 0;
		for (int i = 0; i < hex.length(); i += 2) {
			res[m++] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
		}

		return res;
	}

	public static byte[] hash(final String algorithm, final InputStream in) throws NoSuchAlgorithmException, IOException {
		return hash(algorithm, in, new NullProgressMonitor());
	}

	public static byte[] hash(final String algorithm, final InputStream in, final ProgressMonitor monitor) throws NoSuchAlgorithmException, IOException {
		monitor.beginTask(algorithm, Math.max(1, in.available()));
		try {
			final MessageDigest md = MessageDigest.getInstance(algorithm);
			final byte[] data = new byte[10240];
			while (true) {
				final int bytesRead = in.read(data, 0, data.length);
				if (bytesRead < 0) {
					break;
				}
				if (bytesRead > 0) {
					md.update(data, 0, bytesRead);
					monitor.worked(bytesRead);
				}
			}
			return md.digest();
		} finally {
			monitor.done();
		}
	}

	public static String formatEncodedHexStrForHuman(String hex) {
		if (hex.length() % 2 != 0)
			throw new IllegalArgumentException("The hex string must have an even number of characters!");

		hex = hex.toUpperCase(Locale.UK);

		final StringBuilder sb = new StringBuilder(hex.length() * 3 / 2);
		for (int i = 0; i < hex.length(); i += 2) {
			if (sb.length() > 0)
				sb.append(':');

			sb.append(hex.substring(i, i + 2));
		}
		return sb.toString();
	}

	public static String sha1ForHuman(final byte[] in) {
		try {
			return sha1ForHuman(new ByteArrayInputStream(in));
		} catch (final IOException x) {
			throw new RuntimeException(x);
		}
	}

	public static String sha1ForHuman(final InputStream in) throws IOException {
		return formatEncodedHexStrForHuman(sha1(in));
	}

	public static String sha1(final String in) {
		try {
			return sha1(in.getBytes(IOUtil.CHARSET_NAME_UTF_8));
		} catch (final UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	public static String sha1(final byte[] in) {
		try {
			return sha1(new ByteArrayInputStream(in));
		} catch (final IOException x) {
			throw new RuntimeException(x);
		}
	}

	public static String sha1(final InputStream in) throws IOException {
		byte[] hash;
		try {
			hash = hash(HASH_ALGORITHM_SHA, in);
		} catch (final NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		return encodeHexStr(hash);
	}

}
