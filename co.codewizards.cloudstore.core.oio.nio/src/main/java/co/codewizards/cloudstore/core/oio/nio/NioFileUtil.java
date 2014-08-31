package co.codewizards.cloudstore.core.oio.nio;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import co.codewizards.cloudstore.core.oio.File;

/**
 * @author Sebastian Schefczyk
 *
 */
public final class NioFileUtil {

	private NioFileUtil() { }

	public static File createTempDirectory(final String prefix) throws IOException {
		return createFile(Files.createTempDirectory(prefix).toFile());
	}

	public static File createTempFile(final String prefix, final String suffix) throws IOException {
		return createFile(java.io.File.createTempFile (prefix, suffix));
	}

	public static File createTempFile(final String prefix, final String suffix, final File parentDir) throws IOException {
		return createFile(java.io.File.createTempFile (prefix, suffix, castOrFail(parentDir).getIoFile()));
	}

	static NioFile castOrFail(final File file) {
		if (file instanceof NioFile)
			return (NioFile) file;
		else
			throw new IllegalArgumentException("Could not cast file: "
					+ file.getClass().getCanonicalName());
	}

	static String toPathString(final Path path) {
		assertNotNull("path", path);
		return path.toString().replace(java.io.File.separatorChar, '/');
	}

	static String toPathString(final java.io.File file) {
		final Path path = file.toPath();
		assertNotNull("path", path);
		return path.toString().replace(java.io.File.separatorChar, '/');
	}

	static File[] convert(final java.io.File[] ioFilesListFiles) {
		if (ioFilesListFiles == null)
			return null;
		final File[] listFiles = new NioFile[ioFilesListFiles.length];
		for (int i = 0; i < ioFilesListFiles.length; i++) {
			listFiles[i] = new NioFile(ioFilesListFiles[i]);
		}
		return listFiles;
	}
}
