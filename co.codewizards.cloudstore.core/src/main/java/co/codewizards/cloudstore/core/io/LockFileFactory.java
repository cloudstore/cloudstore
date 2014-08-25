package co.codewizards.cloudstore.core.io;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.oio.File;

/**
 * Factory creating {@link LockFile} instances.
 * <p>
 * All methods of this class are thread-safe.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public class LockFileFactory {

	private static final Logger logger = LoggerFactory.getLogger(LockFileFactory.class);

	private static class LockFileFactoryHolder {
		public static final LockFileFactory instance = new LockFileFactory();
	}

	private final Object mutex = this;

	protected LockFileFactory() { }

	public static LockFileFactory getInstance() {
		return LockFileFactoryHolder.instance;
	}

	private final Map<File, LockFileImpl> file2LockFileImpl = new HashMap<File, LockFileImpl>();

	/**
	 * Acquire an exclusive lock on the specified file.
	 * <p>
	 * <b>Important:</b> You <i>must</i> invoke {@link LockFile#release()} on the returned object! Use a try-finally-block
	 * to ensure it:
	 * <pre>  LockFile lockFile = LockFileFactory.getInstance().acquire(theFile, theTimeout);
	 *  try {
	 *    // do something
	 *  } finally {
	 *    lockFile.release();
	 *  }</pre>
	 * <p>
	 * Since Java 7, it is alternatively possible to use the try-with-resources clause like this:
	 * <pre>  try ( LockFile lockFile = LockFileFactory.getInstance().acquire(theFile, theTimeout); ) {
	 *    // do something while the file represented by 'lockFile' is locked.
	 *  }</pre>
	 * <p>
	 * If the JVM is interrupted or shut down before {@code release()}, the file-lock is released by the
	 * operating system, but a missing {@code release()} causes the file to be locked for the entire remaining runtime
	 * of the JVM! This problem does not exist using the new try-with-resources-clause (since Java 7).
	 * <p>
	 * <b>Important:</b> This is <i>not</i> usable for the synchronization of multiple threads within the same Java virtual machine!
	 * Multiple {@link LockFile}s on the same {@link File} are possible within the same JVM! This locking mechanism
	 * only locks against separate processes! Since this implementation is based on {@link java.nio.channels.FileLock FileLock},
	 * please consult its Javadoc for further information.
	 * <p>
	 * To make it possible to synchronise multiple threads in the same JVM, too, there's {@link LockFile#getLock()}.
	 * <p>
	 * Multiple invocations of this method on the same given {@code file} return multiple different {@code LockFile} instances.
	 * The actual lock is held until the last {@code LockFile} instance was {@linkplain LockFile#release() released}.
	 * <p>
	 * This method is thread-safe.
	 * @param file the file to be locked. Must not be <code>null</code>. If this file does not exist in the file system,
	 * it is created by this method.
	 * @param timeoutMillis the timeout to wait for the lock to be acquired in milliseconds. The value 0 means to
	 * wait forever.
	 * @return the {@code LockFile}. Never <code>null</code>. This <i>must</i> be
	 * {@linkplain java.nio.channels.FileLock#release() released}
	 * (use a try-finally-block)!
	 * @throws TimeoutException if the {@code LockFile} could not be acquired within the timeout specified by {@code timeoutMillis}.
	 * @see LockFile#release()
	 */
	public LockFile acquire(File file, final long timeoutMillis) throws TimeoutException {
		assertNotNull("file", file);
		try {
			file = file.getCanonicalFile();
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}

		LockFileImpl lockFileImpl;
		synchronized (mutex) {
			lockFileImpl = file2LockFileImpl.get(file);
			if (lockFileImpl == null) {
				lockFileImpl = new LockFileImpl(this, file);
				file2LockFileImpl.put(file, lockFileImpl);
				logger.trace("acquire: Adding file='{}' lockFileImpl={}", file, lockFileImpl);
			}
			++lockFileImpl.acquireRunningCounter;
		}
		boolean exceptionThrown = true;
		try {
			// The following must NOT be synchronised! Otherwise we might wait here longer than the current timeout
			// (as long as the longest timeout of all acquire methods running concurrently).
			lockFileImpl.acquire(timeoutMillis);
			exceptionThrown = false;
		} finally {
			synchronized (mutex) {
				final int lockCounter = lockFileImpl.getLockCounter();
				final int acquireRunningCounter = --lockFileImpl.acquireRunningCounter;

				if (lockCounter < 1 && acquireRunningCounter < 1) {
					logger.trace("acquire: Removing lockFileImpl={}", lockFileImpl);
					final LockFileImpl removed = file2LockFileImpl.remove(file);
					if (removed != lockFileImpl)
						throw new IllegalStateException(String.format("file2LockFileImpl.remove(file) != lockFileImpl :: %s != %s", removed, lockFileImpl));
				}

				if (lockCounter < 1 && ! exceptionThrown)
					throw new IllegalStateException("lockCounter < 1, but no exception thrown!");
			}
		}
		return new LockFileProxy(lockFileImpl);
	}

	/**
	 * Callback from {@link LockFileImpl#release()}.
	 * @param lockFileImpl the {@code LockFileImpl} which notifies this factory about being released.
	 */
	protected void postRelease(final LockFileImpl lockFileImpl) {
		synchronized (mutex) {
			final LockFileImpl lockFileImpl2 = file2LockFileImpl.get(lockFileImpl.getFile());
			if (lockFileImpl != lockFileImpl2)
				throw new IllegalArgumentException(String.format("Unknown lockFileImpl instance (not managed by this registry)! file2LockFileImpl.get(lockFileImpl.getFile()) != lockFileImpl :: %s != %s ", lockFileImpl2, lockFileImpl));

			final int lockCounter = lockFileImpl.getLockCounter();
			final int acquireRunningCounter = lockFileImpl.acquireRunningCounter;

			if (lockCounter < 1 && acquireRunningCounter < 1) {
				logger.trace("postRelease: Removing lockFileImpl={}", lockFileImpl);
				final LockFileImpl removed = file2LockFileImpl.remove(lockFileImpl.getFile());
				if (removed != lockFileImpl)
					throw new IllegalStateException(String.format("file2LockFileImpl.remove(file) != lockFileImpl :: %s != %s", removed, lockFileImpl));
			}
		}
	}

}
