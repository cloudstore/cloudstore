package co.codewizards.cloudstore.core.oio;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

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
	public static File createFile(final String parent, final String ... children) {
		assertNotNull(parent, "parent");

		final FileFactory fileFactory = OioRegistry.getInstance().getFileFactory();
		File result = null;
		if (children != null) {
			for (final String child : children) {
				if (result == null)
					result = fileFactory.createFile(parent, child);
				else
					result = fileFactory.createFile(result, child);
			}
		}

		if (result == null)
			result = createFile(parent);

		return result;
	}

	/** Factory method, substitutes the constructor of {@link java.io.File}. */
	public static File createFile(final File parent, final String ... children) {
		assertNotNull(parent, "parent");

		final FileFactory fileFactory = OioRegistry.getInstance().getFileFactory();
		File result = parent;
		if (children != null) {
			for (final String child : children)
				result = fileFactory.createFile(result, child);
		}
		return result;
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

	/**
	 * Lists the file system roots (wraps {@link java.io.File#listRoots()}).
	 * @return the file system roots. Never <code>null</code>.
	 */
	public static File[] listRootFiles() {
		return OioRegistry.getInstance().getFileFactory().listRootFiles();
	}
}
