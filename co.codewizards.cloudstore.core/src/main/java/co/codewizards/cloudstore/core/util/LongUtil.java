package co.codewizards.cloudstore.core.util;

import static java.util.Objects.*;

public final class LongUtil {

	private LongUtil() {
	}

	public static final byte[] toBytes(final long value) {
		final byte[] bytes = new byte[8];

		int idx = -1;
		for (int i = 7; i >= 0; --i)
			bytes[++idx] = (byte) (value >> (8 * i));

		return bytes;
	}

	public static final long fromBytes(final byte[] bytes) {
		requireNonNull(bytes, "bytes");
		long value = 0;

		final int imax = Math.min(8, bytes.length);
		for (int i = 0; i < imax; ++i)
			value = (value << 8) | (bytes[i] & 0xff);

		return value;
	}

	public static final String[] toBytesHex(final long value, final boolean withZeroPadding) {
		final byte[] bytes = toBytes(value);
		String[] result = new String[bytes.length];
		for (int i = 0; i < bytes.length; i++) {
			result[i] = Integer.toHexString(bytes[i] & 0xff);

			if (withZeroPadding && result[i].length() == 1)
				result[i] = '0' + result[i];
		}
		return result;
	}

	public static final long fromBytesHex(final String[] bytesHex) {
		requireNonNull(bytesHex, "bytesHex");
		final byte[] bytes = new byte[bytesHex.length];
		for (int i = 0; i < bytesHex.length; i++)
			bytes[i] = (byte) Integer.parseInt(bytesHex[i], 16);

		return fromBytes(bytes);
	}
}
