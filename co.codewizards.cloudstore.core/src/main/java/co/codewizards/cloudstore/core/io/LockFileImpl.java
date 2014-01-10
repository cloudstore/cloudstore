package co.codewizards.cloudstore.core.io;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.util.concurrent.atomic.AtomicInteger;

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

}
