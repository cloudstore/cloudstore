package co.codewizards.cloudstore.core.io;

import static co.codewizards.cloudstore.core.util.Util.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class LockFileProxy implements LockFile {
	private static final Logger logger = LoggerFactory.getLogger(LockFileProxy.class);

	private final LockFileImpl lockFileImpl;
	private final AtomicBoolean released = new AtomicBoolean(false);

	public LockFileProxy(final LockFileImpl lockFileImpl) {
		this.lockFileImpl = assertNotNull("lockFileImpl", lockFileImpl);
	}

	@Override
	public File getFile() {
		return lockFileImpl.getFile();
	}

	@Override
	public void release() {
		if (!released.compareAndSet(false, true)) {
			final IllegalStateException x = new IllegalStateException("Multiple invocations of release() should be avoided!");
			logger.warn(x.toString(), x);
			return;
		}
		lockFileImpl.release();
	}

	@Override
	public final void close() {
		release();
	}

	public LockFileImpl getLockFileImpl() {
		return lockFileImpl;
	}

	@Override
	public Lock getLock() {
		return lockFileImpl.getLock();
	}

	@Override
	public InputStream createInputStream() throws IOException {
		return lockFileImpl.createInputStream();
	}

	@Override
	public OutputStream createOutputStream() throws IOException {
		return lockFileImpl.createOutputStream();
	}
}
