package co.codewizards.cloudstore.oio.nio;

import java.io.IOException;

import co.codewizards.cloudstore.core.oio.FileChannel;
import co.codewizards.cloudstore.core.oio.FileLock;

/**
 * @author Sebastian Schefczyk
 *
 */
public class NioFileChannel implements FileChannel {

	private final java.nio.channels.FileChannel channel;

	public NioFileChannel(final java.nio.channels.FileChannel channel) {
		this.channel = channel;
	}

	@Override
	public FileLock tryLock(final long position, final long size, final boolean shared) throws IOException {
		return new NioFileLock(channel.tryLock(position, size, shared));
	}

}
