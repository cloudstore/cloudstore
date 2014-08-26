package co.codewizards.cloudstore.core.oio;

import java.io.IOException;

/**
 * Substitute for java.nio.channels.FileChannel.
 *
 * @author Sebastian Schefczyk
 */
public interface FileChannel {

	FileLock tryLock(final long position, final long size, final boolean shared) throws IOException;

}
