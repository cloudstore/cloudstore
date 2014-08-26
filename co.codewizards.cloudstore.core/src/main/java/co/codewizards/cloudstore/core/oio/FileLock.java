package co.codewizards.cloudstore.core.oio;

import java.io.IOException;

/**
 * Substitute for java.nio.channels.FileLock.
 *
 * @author Sebastian Schefczyk
 */
public interface FileLock {

	void release() throws IOException;

}
