package co.codewizards.cloudstore.core.io;

/**
 * Subclass of {@link java.io.ByteArrayInputStream} also implementing {@link IByteArrayInputStream}.
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public class ByteArrayInputStream extends java.io.ByteArrayInputStream implements IByteArrayInputStream {

	public ByteArrayInputStream(byte[] buf) {
		super(buf);
	}

	public ByteArrayInputStream(byte[] buf, int offset, int length) {
		super(buf, offset, length);
	}

}
