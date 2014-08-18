package co.codewizards.cloudstore.oio.nio;

import java.io.IOException;
import java.nio.file.Files;

import co.codewizards.cloudstore.core.oio.file.File;

/**
 * @author Sebastian Schefczyk
 *
 */
public final class NioFileUtil {

	private NioFileUtil() { }

	public static File createTempDirectory(final String prefix) throws IOException {
		return new NioFile(Files.createTempDirectory(prefix).toFile());
	}

	public static File createTempFile(final String prefix, final String suffix) throws IOException {
		return new NioFile(java.io.File.createTempFile (prefix, suffix));
	}

	public static File createTempFile(final String prefix, final String suffix, final File dir) throws IOException {
		return new NioFile(java.io.File.createTempFile (prefix, suffix, castOrFail(dir).ioFile));
	}

	static NioFile castOrFail(final File file) {
		if (file instanceof NioFile)
			return (NioFile) file;
		else
			throw new IllegalArgumentException("Could not cast file: "
					+ file.getClass().getCanonicalName());
	}
}
