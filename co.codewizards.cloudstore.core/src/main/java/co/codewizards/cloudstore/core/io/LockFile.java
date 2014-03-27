package co.codewizards.cloudstore.core.io;

import java.io.File;

/**
 * Lock-file exclusively locking a certain {@link #getFile() file}.
 * <p>
 * An instance is acquired by invoking {@link LockFileFactory#acquire(File, long)}.
 * <p>
 * All methods of this interface are thread-safe.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public interface LockFile {

	/**
	 * Gets the underlying file being locked.
	 * @return the underlying file being locked. Never <code>null</code>.
	 */
	File getFile();

	/**
	 * Releases the lock.
	 * <p>
	 * <b>Important:</b> This method <b>must</b> be called <b>exactly once</b> for every {@code LockFile} instance!
	 * It is highly recommended to use a try-finally-block:
	 * <pre>  LockFile lockFile = LockFileFactory.acquire(theFile, theTimeout);
	 *  try {
	 *    // do something
	 *  } finally {
	 *    lockFile.release();
	 *  }</pre>
	 *  <p>
	 *  This method is thread-safe and thus might be invoked on a different thread than the instance
	 *  was created. However, it must be invoked exactly once (per {@code LockFile} instance).
	 *  @see LockFileFactory#acquire(File, long)
	 *  @throws IllegalStateException if this method is invoked more than once on the same instance.
	 */
	void release();

}
