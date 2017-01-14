package co.codewizards.cloudstore.core.io;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.util.AssertUtil;

class LockFileProxy implements LockFile {
	private static final Logger logger = LoggerFactory.getLogger(LockFileProxy.class);

	private final LockFileImpl lockFileImpl;
	private final AtomicBoolean released = new AtomicBoolean(false);

	public LockFileProxy(final LockFileImpl lockFileImpl) {
		this.lockFileImpl = AssertUtil.assertNotNull(lockFileImpl, "lockFileImpl");
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
	public IInputStream createInputStream() throws IOException {
		return lockFileImpl.createInputStream();
	}

	@Override
	public IOutputStream createOutputStream() throws IOException {
		return lockFileImpl.createOutputStream();
	}
}
