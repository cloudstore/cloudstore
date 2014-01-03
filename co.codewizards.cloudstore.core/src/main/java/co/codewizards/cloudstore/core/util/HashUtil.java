package co.codewizards.cloudstore.core.util;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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
	public static String encodeHexStr(byte[] buf, int pos, int len)
	{
		 StringBuilder hex = new StringBuilder();
		 while (len-- > 0) {
				byte ch = buf[pos++];
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
	public static byte[] decodeHexStr(String hex)
	{
		if (hex.length() % 2 != 0)
			throw new IllegalArgumentException("The hex string must have an even number of characters!");

		byte[] res = new byte[hex.length() / 2];

		int m = 0;
		for (int i = 0; i < hex.length(); i += 2) {
			res[m++] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
		}

		return res;
	}

	public static byte[] hash(String algorithm, InputStream in) throws NoSuchAlgorithmException, IOException {
		MessageDigest md = MessageDigest.getInstance(algorithm);
		byte[] data = new byte[10240];
		while (true) {
			int bytesRead = in.read(data, 0, data.length);
			if (bytesRead < 0) {
				break;
			}
			if (bytesRead > 0)
				md.update(data, 0, bytesRead);
		}
		return md.digest();
	}
}
