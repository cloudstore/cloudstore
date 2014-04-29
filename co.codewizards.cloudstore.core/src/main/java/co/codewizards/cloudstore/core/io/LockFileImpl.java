package co.codewizards.cloudstore.core.io;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class LockFileImpl implements LockFile {
	private static final Logger logger = LoggerFactory.getLogger(LockFileImpl.class);

	private final LockFileFactory lockFileFactory;
	private final File file;
	private final String thisID = Integer.toHexString(System.identityHashCode(this));

	protected final AtomicInteger acquireRunningCounter = new AtomicInteger();

	private int lockCounter = 0;
	private RandomAccessFile randomAccessFile;
	private FileLock fileLock;
	private final Lock lock = new ReentrantLock();

	protected LockFileImpl(LockFileFactory lockFileFactory, File file) {
		this.lockFileFactory = assertNotNull("lockFileFactory", lockFileFactory);
		this.file = assertNotNull("file", file);
		logger.debug("[{}]<init>: file='{}'", thisID, file);
	}

	@Override
	public File getFile() {
		return file;
	}

	private boolean tryAcquire() {
		logger.trace("[{}]tryAcquire: entered. lockCounter={}", thisID, lockCounter);
		synchronized (this) {
			logger.trace("[{}]tryAcquire: inside synchronized", thisID);
			try {
				if (randomAccessFile == null) {
					logger.trace("[{}]tryAcquire: acquiring underlying FileLock.", thisID);
					randomAccessFile = new RandomAccessFile(file, "rw");
					try {
						fileLock = randomAccessFile.getChannel().tryLock(0, Long.MAX_VALUE, false);
					} finally {
						if (fileLock == null) {
							logger.trace("[{}]tryAcquire: fileLock was NOT acquired. Closing randomAccessFile now.", thisID);
							randomAccessFile.close();
							randomAccessFile = null;
						}
					}
					if (fileLock == null) {
						logger.debug("[{}]tryAcquire: returning false. lockCounter={}", thisID, lockCounter);
						return false;
					}
				}
				++lockCounter;
				logger.debug("[{}]tryAcquire: returning true. lockCounter={}", thisID, lockCounter);
				return true;
			} catch (IOException x) {
				throw new RuntimeException(x);
			}
		}
	}

	public void acquire(long timeoutMillis) throws TimeoutException {
		if (timeoutMillis < 0)
			throw new IllegalArgumentException("timeoutMillis < 0");

		long beginTimestamp = System.currentTimeMillis();

		while (!tryAcquire()) {
			try {
				Thread.sleep(300);
			} catch (InterruptedException e) { doNothing(); }

			if (timeoutMillis > 0 && System.currentTimeMillis() - beginTimestamp > timeoutMillis) {
				throw new TimeoutException(String.format("Could not lock '%s' within timeout of %s ms!",
						file.getAbsolutePath(), timeoutMillis));
			}
		}
	}

	/**
	 * Releases the lock.
	 * <p>
	 * <b>Important:</b> In contrast to the documentation of the API method {@link LockFile#release()}, the
	 * implementation of this method in {@link LockFileImpl} is invoked multiple times: Once
	 * for every {@link LockFile}-API-instance (i.e. {@link LockFileProxy} instance). The actual implementation
	 * uses reference-counting to know when to release the real, underlying lock.
	 */
	@Override
	public void release() {
		logger.trace("[{}]release: entered. lockCounter={}", thisID, lockCounter);
		synchronized (this) {
			logger.trace("[{}]release: inside synchronized", thisID);
			int lockCounterValue = --lockCounter;
			if (lockCounterValue > 0) {
				logger.debug("[{}]release: NOT releasing underlying FileLock. lockCounter={}", thisID, lockCounter);
				return;
			}

			if (lockCounterValue < 0)
				throw new IllegalStateException("Trying to release more often than was acquired!!!");

			logger.debug("[{}]release: releasing underlying FileLock. lockCounter={}", thisID, lockCounter);
			try {
				if (fileLock != null) {
					fileLock.release();
					fileLock = null;
				}

				if (randomAccessFile != null) {
					randomAccessFile.close();
					randomAccessFile = null;
				}
			} catch (IOException x) {
				throw new RuntimeException(x);
			}
		}
		lockFileFactory.postRelease(this);
	}

	protected int getLockCounter() {
		return lockCounter;
	}

	private static void doNothing() { }

	@Override
	public Lock getLock() {
		return lock;
	}

	@Override
	public InputStream createInputStream() throws IOException {
		return new LockFileInputStream();
	}

	@Override
	public OutputStream createOutputStream() throws IOException {
		return new LockFileOutputStream();
	}

	private class LockFileInputStream extends InputStream {

		private long position;

		public LockFileInputStream() {
			lock.lock();
		}

		@Override
		public int read() throws IOException {
			randomAccessFile.seek(position);
			final int result = randomAccessFile.read();
			position = randomAccessFile.getFilePointer();
			return result;
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			randomAccessFile.seek(position);
			final int result = randomAccessFile.read(b, off, len);
			position = randomAccessFile.getFilePointer();
			return result;
		}

		@Override
		public void close() throws IOException {
			super.close();
			lock.unlock();
		}
	}

	private class LockFileOutputStream extends OutputStream {

		private long position;

		public LockFileOutputStream() throws IOException {
			lock.lock();
			randomAccessFile.setLength(0);
		}

		@Override
		public void write(int b) throws IOException {
			randomAccessFile.seek(position);
			randomAccessFile.write(b);
			position = randomAccessFile.getFilePointer();
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			randomAccessFile.seek(position);
			randomAccessFile.write(b, off, len);
			position = randomAccessFile.getFilePointer();
		}

		@Override
		public void close() throws IOException {
			super.close();
			lock.unlock();
		}
	}
}
