package co.codewizards.cloudstore.core.io;

import static co.codewizards.cloudstore.core.io.StreamUtil.*;

import java.io.IOException;

/**
 * Subclass of {@link java.io.ByteArrayOutputStream} also implementing {@link IByteArrayOutputStream}.
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public class ByteArrayOutputStream extends java.io.ByteArrayOutputStream implements IByteArrayOutputStream {

	public ByteArrayOutputStream() {
	}

	public ByteArrayOutputStream(int size) {
		super(size);
	}

	@Override
	public void writeTo(IOutputStream out) throws IOException {
		writeTo(castStream(out));
	}
}
