package co.codewizards.cloudstore.core.oio.file;

import java.io.IOException;
import java.net.URI;

/**
 * @author Sebastian Schefczyk
 *
 */
public interface FileFactoryService extends OioService {

	File createNewFile(final String pathname);

	File createNewFile(final String parent, final String child);

	File createNewFile(final File parent, final String child);

	File createNewFile(final java.io.File file);

	File createNewFile(final URI uri);


	File createTempDirectory(String prefix) throws IOException;

	File createTempFile(String prefix, String suffix) throws IOException;

	File createTempFile(String prefix, String suffix, File dir) throws IOException;
}
