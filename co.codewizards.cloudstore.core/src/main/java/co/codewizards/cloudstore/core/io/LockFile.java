package co.codewizards.cloudstore.core.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.locks.Lock;

import co.codewizards.cloudstore.core.oio.File;

/**
 * Lock-file exclusively locking a certain {@link #getFile() file}.
 * <p>
 * An instance is acquired by invoking {@link LockFileFactory#acquire(File, long)}.
 * <p>
 * All methods of this interface are thread-safe.
 * <p>
 * <b>Important:</b> You should never access the {@linkplain #getFile() locked file} directly. Though this
 * works fine on some operating systems (e.g. on GNU/Linux), it fails on others (e.g. on Windows). If you
 * want to read from / write to the locked file, please use {@link #createInputStream()} or
 * {@link #createOutputStream()} instead.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public interface LockFile extends AutoCloseable {

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
	 *  was created. However, it should be invoked exactly once (per {@code LockFile} instance).
	 *  <p>
	 *  Please note: It is possible to use the new try-with-resources-clause introduced by Java 7. See
	 *  {@link #close()}.
	 *  @see LockFileFactory#acquire(File, long)
	 *  @see #close()
	 */
	void release();

	/**
	 * Equivalent to {@link #release()}.
	 * <p>
	 * Implementations must make sure that invoking {@code close()} means exactly the same as invoking
	 * {@code release()}. This method was added to make the usage of {@code LockFile} possible in a
	 * try-with-resources-clause. See {@link AutoCloseable} for more details. Here's a code example:
	 * <pre>  try ( LockFile lockFile = LockFileFactory.acquire(theFile, theTimeout); ) {
	 *    // do something while the file represented by 'lockFile' is locked.
	 *  }</pre>
	 * <p>
	 * @see #release()
	 */
	@Override
	public void close();

	/**
	 * Gets the {@code Lock} corresponding to the underlying file to synchronise multiple threads of the same
	 * process.
	 * <p>
	 * A {@code LockFile} (usually implemented using {@link java.nio.channels.FileLock FileLock}) is not
	 * guaranteed to exclude multiple threads from accessing a single file. In order to additionally provide
	 * thread-synchronisation (as is pretty straight-forward and needed in many situations), there is a
	 * {@link Lock} associated to every {@code LockFile}. It is highly recommended to synchronise additionally
	 * on this {@code Lock}. Note, that {@link #createInputStream()} and {@link #createOutputStream()} are
	 * expected to implicitly do this.
	 * @return the {@code Lock} corresponding to the underlying file. Never <code>null</code>.
	 */
	Lock getLock();

	/**
	 * Creates an {@link InputStream} reading from the {@linkplain #getFile() locked file}.
	 * <p>
	 * <b>Important:</b> You must {@linkplain InputStream#close() close} the {@code InputStream}! Locks held
	 * are released only when doing so.
	 * @return an {@link InputStream} reading from the {@linkplain #getFile() locked file}. Never
	 * <code>null</code>.
	 * @throws IOException if creating the {@link InputStream} fails.
	 */
	IInputStream createInputStream() throws IOException;

	/**
	 * Creates an {@link OutputStream} writing into the {@linkplain #getFile() locked file} (overwriting
	 * all old content).
	 * <p>
	 * <b>Important:</b> You must {@linkplain OutputStream#close() close} the {@code OutputStream}! Locks
	 * held are released only when doing so.
	 * @return an {@link OutputStream} writing into the {@linkplain #getFile() locked file}. Never
	 * <code>null</code>.
	 * @throws IOException if creating the {@link OutputStream} fails.
	 */
	IOutputStream createOutputStream() throws IOException;

}
