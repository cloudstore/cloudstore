package co.codewizards.cloudstore.ls.client.util;

import static java.util.Objects.*;

import co.codewizards.cloudstore.core.io.ByteArrayInputStream;
import co.codewizards.cloudstore.core.io.IByteArrayInputStream;
import co.codewizards.cloudstore.core.io.IByteArrayOutputStream;
import co.codewizards.cloudstore.ls.client.LocalServerClient;

/**
 * Utility class for creating {@link ByteArrayInputStream}s inside the LocalServer's VM.
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public final class ByteArrayInputStreamLs {

	private ByteArrayInputStreamLs() {
	}

	public static IByteArrayInputStream create(byte[] data) {
		requireNonNull(data, "data");
		return LocalServerClient.getInstance().invokeConstructor(ByteArrayInputStream.class,
				new Class<?>[] { byte[].class },
				data);
	}

	public static IByteArrayInputStream create(byte[] data, int offset, int length) {
		requireNonNull(data, "data");
		return LocalServerClient.getInstance().invokeConstructor(ByteArrayInputStream.class,
				new Class<?>[] { byte[].class, int.class, int.class },
				data, offset, length);
	}

	public static IByteArrayInputStream create(IByteArrayOutputStream bout) {
		return create(bout.toByteArray());
	}

	public static IByteArrayInputStream create(IByteArrayOutputStream bout, int offset, int length) {
		return create(bout.toByteArray(), offset, length);
	}
}
