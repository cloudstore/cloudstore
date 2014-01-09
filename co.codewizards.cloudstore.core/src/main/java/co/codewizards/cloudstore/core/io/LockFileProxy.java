package co.codewizards.cloudstore.core.io;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

class LockFileProxy implements LockFile {

	private final LockFileImpl lockFileImpl;
	private AtomicBoolean released = new AtomicBoolean(false);

	public LockFileProxy(LockFileImpl lockFileImpl) {
		this.lockFileImpl = assertNotNull("lockFileImpl", lockFileImpl);
	}

	@Override
	public File getFile() {
		return lockFileImpl.getFile();
	}

	@Override
	public void release() {
		if (!released.compareAndSet(false, true))
			throw new IllegalStateException("Multiple invocations of release() are not allowed!");

		lockFileImpl.release();
	}

	public LockFileImpl getLockFileImpl() {
		return lockFileImpl;
	}

}
