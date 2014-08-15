package co.codewizards.cloudstore.core.oio.file;

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

	public static File newFile(final File file, final String child) {
		return OioProvider.getInstance().file().createNewFile(file, child);
	}


	public static final String FILE_SEPARATOR = java.io.File.separator;

	public static final char FILE_SEPARATOR_CHAR = java.io.File.separatorChar;

}
