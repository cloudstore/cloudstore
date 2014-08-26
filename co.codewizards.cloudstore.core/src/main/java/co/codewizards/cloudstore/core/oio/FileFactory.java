package co.codewizards.cloudstore.core.oio;

import java.io.IOException;
import java.net.URI;

/**
 * @author Sebastian Schefczyk
 */
public interface FileFactory extends FileService {

	File createFile(final String pathname);

	File createFile(final String parent, final String child);

	File createFile(final File parent, final String child);

	File createFile(final java.io.File file);

	File createFile(final URI uri);


	File createTempDirectory(String prefix) throws IOException;

	File createTempFile(String prefix, String suffix) throws IOException;

	File createTempFile(String prefix, String suffix, File dir) throws IOException;
}
