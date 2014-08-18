package co.codewizards.cloudstore.core.oio.file;

import java.io.IOException;
import java.net.URI;


/**
 * @author Sebastian Schefczyk
 *
 */
public class FileFactory {

	public static final String FILE_SEPARATOR = java.io.File.separator;

	public static final char FILE_SEPARATOR_CHAR = java.io.File.separatorChar;


	/** Factory method, substitutes the constructor of {@link java.io.File}. */
	public static File newFile(final String pathname) {
		return OioProvider.getInstance().getFileFactory().createNewFile(pathname);
	}

	/** Factory method, substitutes the constructor of {@link java.io.File}. */
	public static File newFile(final String parent, final String child) {
		return OioProvider.getInstance().getFileFactory().createNewFile(parent, child);
	}

	/** Factory method, substitutes the constructor of {@link java.io.File}. */
	public static File newFile(final File parent, final String child) {
		return OioProvider.getInstance().getFileFactory().createNewFile(parent, child);
	}

	/** Factory method, substitutes the constructor of {@link java.io.File}. */
	public static File newFile(final java.io.File file) {
		return OioProvider.getInstance().getFileFactory().createNewFile(file);
	}

	/** Factory method, substitutes the constructor of {@link java.io.File}. */
	public static File newFile(final URI uri) {
		return OioProvider.getInstance().getFileFactory().createNewFile(uri);
	}

	public static File createTempDirectory(final String prefix) throws IOException {
		return OioProvider.getInstance().getFileFactory().createTempDirectory(prefix);
	}

	public static File createTempFile(final String prefix, final String suffix) throws IOException {
		return OioProvider.getInstance().getFileFactory().createTempFile(prefix, suffix);
	}

	public static File createTempFile(final String prefix, final String suffix, final File dir) throws IOException {
		return OioProvider.getInstance().getFileFactory().createTempFile(prefix, suffix, dir);
	}
}
