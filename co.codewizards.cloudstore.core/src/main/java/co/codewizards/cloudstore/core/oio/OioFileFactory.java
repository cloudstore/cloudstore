package co.codewizards.cloudstore.core.oio;

import java.io.IOException;
import java.net.URI;


/**
 * @author Sebastian Schefczyk
 *
 */
public class OioFileFactory {

	public static final String FILE_SEPARATOR = java.io.File.separator;

	public static final char FILE_SEPARATOR_CHAR = java.io.File.separatorChar;


	/** Factory method, substitutes the constructor of {@link java.io.File}. */
	public static File createFile(final String pathname) {
		return OioRegistry.getInstance().getFileFactory().createFile(pathname);
	}

	/** Factory method, substitutes the constructor of {@link java.io.File}. */
	public static File createFile(final String parent, final String child) {
		return OioRegistry.getInstance().getFileFactory().createFile(parent, child);
	}

	/** Factory method, substitutes the constructor of {@link java.io.File}. */
	public static File createFile(final File parent, final String child) {
		return OioRegistry.getInstance().getFileFactory().createFile(parent, child);
	}

	/** Factory method, substitutes the constructor of {@link java.io.File}. */
	public static File createFile(final java.io.File file) {
		return OioRegistry.getInstance().getFileFactory().createFile(file);
	}

	/** Factory method, substitutes the constructor of {@link java.io.File}. */
	public static File createFile(final URI uri) {
		return OioRegistry.getInstance().getFileFactory().createFile(uri);
	}

	/** Creates a temporary directory. */
	public static File createTempDirectory(final String prefix) throws IOException {
		return OioRegistry.getInstance().getFileFactory().createTempDirectory(prefix);
	}

	/** Creates a temporary file. */
	public static File createTempFile(final String prefix, final String suffix) throws IOException {
		return OioRegistry.getInstance().getFileFactory().createTempFile(prefix, suffix);
	}
	/** Creates a temporary file within specified parent directory. */
	public static File createTempFile(final String prefix, final String suffix, final File dir) throws IOException {
		return OioRegistry.getInstance().getFileFactory().createTempFile(prefix, suffix, dir);
	}
}
