package co.codewizards.cloudstore.core.util;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;

/**
 * @author Sebastian Schefczyk
 *
 */
public final class StreamUtil {

	private StreamUtil() {}

	/**
	 * Permits to check the InputStream is empty or not. Please note that only
	 * the returned InputStream must be consummed.
	 *
	 * See: http://stackoverflow.com/questions/1524299/how-can-i-check-if-an-
	 * inputstream-is-empty-without-reading-from-it
	 */
	public static InputStream checkStreamIsNotEmpty(final InputStream inputStream)
			throws IOException, EmptyInputStreamException {
		assertNotNull("inputStream", inputStream);
		final PushbackInputStream pushbackInputStream = new PushbackInputStream(
				inputStream);
		int b;
		b = pushbackInputStream.read();
		if (b == -1) {
			throw new EmptyInputStreamException("No byte can be read from stream " + inputStream);
		}
		pushbackInputStream.unread(b);
		return pushbackInputStream;
	}

}
