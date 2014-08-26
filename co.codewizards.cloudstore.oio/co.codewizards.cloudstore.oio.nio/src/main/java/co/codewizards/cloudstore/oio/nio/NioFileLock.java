package co.codewizards.cloudstore.oio.nio;

import java.io.IOException;

import co.codewizards.cloudstore.core.oio.FileLock;

public class NioFileLock implements FileLock {

	private final java.nio.channels.FileLock fileLock;

	public NioFileLock(final java.nio.channels.FileLock tryLock) {
		this.fileLock = tryLock;
	}

	@Override
	public void release() throws IOException {
		fileLock.release();
	}

}
