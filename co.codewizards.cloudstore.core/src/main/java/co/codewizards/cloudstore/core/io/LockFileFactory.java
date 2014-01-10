package co.codewizards.cloudstore.core.io;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileLock;
import java.util.HashMap;
import java.util.Map;

public class LockFileFactory {

	private static class LockFileFactoryHolder {
		public static final LockFileFactory instance = new LockFileFactory();
	}

	protected final Object mutex = new Object();

	protected LockFileFactory() { }

	public static LockFileFactory getInstance() {
		return LockFileFactoryHolder.instance;
	}

	private Map<File, LockFileImpl> file2LockFileImpl = new HashMap<File, LockFileImpl>();

	/**
	 * Acquire an exclusive lock on the specified file.
	 * <p>
	 * <b>Important:</b> You <i>must</i> invoke {@link LockFile#release()} on the returned object! Use a try-finally-block
	 * to ensure it. If the JVM is interrupted or shut down before {@code release()}, the file-lock is released by the
	 * operating system, but a missing {@code release()} causes the file to be locked for the entire remaining runtime
	 * of the JVM!
	 * <p>
	 * <b>Important:</b> This is <i>not</i> usable for the synchronization of multiple threads within the same Java virtual machine!
	 * Multiple {@link LockFile}s on the same {@link File} are possible within the same JVM! This locking mechanism
	 * only locks against separate processes! Since this implementation is based on {@link FileLock}, please consult
	 * its Javadoc for further information.
	 * @param file the file to be locked. Must not be <code>null</code>.
	 * @param timeoutMillis the timeout to wait for the lock to be acquired in milliseconds. The value 0 means to
	 * wait forever.
	 * @return the {@code LockFile}. Never <code>null</code>. This <i>must</i> be {@linkplain FileLock#release() released}
	 * (use a try-finally-block)!
	 * @throws TimeoutException if the {@code LockFile} could not be acquired within the timeout specified by {@code timeoutMillis}.
	 */
	public LockFile acquire(File file, long timeoutMillis) throws TimeoutException {
		assertNotNull("file", file);
		try {
			file = file.getCanonicalFile();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		LockFileImpl lockFileImpl;
		synchronized (this) {
			lockFileImpl = file2LockFileImpl.get(file);
			if (lockFileImpl == null) {
				lockFileImpl = new LockFileImpl(this, file);
				file2LockFileImpl.put(file, lockFileImpl);
			}
			lockFileImpl.acquireRunningCounter.incrementAndGet();
		}
		try {
			// The following must NOT be synchronized! Otherwise we might wait here longer than the current timeout
			// (as long as the longest timeout of all acquire methods running concurrently).
			lockFileImpl.acquire(timeoutMillis);
		} finally {
			synchronized (this) {
				int lockCounter = lockFileImpl.getLockCounter();
				int acquireRunningCounter = lockFileImpl.acquireRunningCounter.decrementAndGet();

				if (lockCounter < 1 && acquireRunningCounter < 1)
					file2LockFileImpl.remove(file);
			}
		}
		return new LockFileProxy(lockFileImpl);
	}

	protected synchronized void postRelease(LockFileImpl lockFileImpl) {
		LockFileImpl lockFileImpl2 = file2LockFileImpl.get(lockFileImpl.getFile());
		if (lockFileImpl != lockFileImpl2)
			throw new IllegalArgumentException("Unknown lockFileImpl instance (not managed by this registry): " + lockFileImpl);

		int lockCounter = lockFileImpl.getLockCounter();
		int acquireRunningCounter = lockFileImpl.acquireRunningCounter.decrementAndGet();

		if (lockCounter < 1 && acquireRunningCounter < 1)
			file2LockFileImpl.remove(lockFileImpl.getFile());
	}

}
