package co.codewizards.cloudstore.ls.client.util;

import co.codewizards.cloudstore.core.io.ByteArrayOutputStream;
import co.codewizards.cloudstore.core.io.IByteArrayOutputStream;
import co.codewizards.cloudstore.ls.client.LocalServerClient;

/**
 * Utility class for creating {@link ByteArrayOutputStream}s inside the LocalServer's VM.
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public final class ByteArrayOutputStreamLs {

	private ByteArrayOutputStreamLs() {
	}

	public static IByteArrayOutputStream create() {
		return LocalServerClient.getInstance().invokeConstructor(ByteArrayOutputStream.class);
	}

	public static IByteArrayOutputStream create(int size) {
		return LocalServerClient.getInstance().invokeConstructor(ByteArrayOutputStream.class,
				new Class<?>[] { int.class },
				size);
	}
}
