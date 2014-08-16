package co.codewizards.cloudstore.core.oio.file;

import java.io.IOException;
import java.net.URI;

/**
 * @author Sebastian Schefczyk
 *
 */
public class FileFactory {

	public static File newFile(final String fileName) {
		return OioProvider.getInstance().file().createNewFile(fileName);
	}

	public static File newFile(final URI uri) {
		return OioProvider.getInstance().file().createNewFile(uri);
	}

	public static File newFile(final String parent, final String child) {
		return OioProvider.getInstance().file().createNewFile(parent, child);
	}

	public static File newFile(final java.io.File file) {
		return OioProvider.getInstance().file().createNewFile(file);
	}

	public static File newFile(final File parent, final String child) {
		return OioProvider.getInstance().file().createNewFile(parent, child);
	}

	public static File createTempDirectory(final String prefix) throws IOException {
		return OioProvider.getInstance().file().createTempDirectory(prefix);
	}

	public static File createTempFile(final String prefix, final String suffix) throws IOException {
		return OioProvider.getInstance().file().createTempFile(prefix, suffix);
	}

	public static File createTempFile(final String prefix, final String suffix, final File dir) throws IOException {
		return OioProvider.getInstance().file().createTempFile(prefix, suffix, dir);
	}

	public static final String FILE_SEPARATOR = java.io.File.separator;

	public static final char FILE_SEPARATOR_CHAR = java.io.File.separatorChar;

}
