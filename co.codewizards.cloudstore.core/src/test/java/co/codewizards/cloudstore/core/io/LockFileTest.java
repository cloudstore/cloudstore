package co.codewizards.cloudstore.core.io;

import static co.codewizards.cloudstore.core.util.Util.doNothing;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import co.codewizards.cloudstore.core.util.IOUtil;

public class LockFileTest {

	private static Random random = new Random();

	@Test
	public void acquireAndReleaseMultipleInstances() {
		final File file = new File(IOUtil.getTempDir(), Long.toString(System.currentTimeMillis(), 36));
		try ( LockFile lockFile1 = LockFileFactory.getInstance().acquire(file, 10000); ) {
			try ( LockFile lockFile2 = LockFileFactory.getInstance().acquire(file, 10000); ) {
				System.out.println("Test");
			}
		}
	}

	@Test
	public void multiThreadAcquireAndRelease() throws Exception {
		final File file = new File(IOUtil.getTempDir(), Long.toString(System.currentTimeMillis(), 36));

		final List<LockFileTestThread> threads = new LinkedList<LockFileTestThread>();
		for (int i = 0; i < 10 + random.nextInt(90); ++i) {
			final LockFileTestThread thread = new LockFileTestThread(i, file);
			thread.start();
			threads.add(thread);
		}

		for (final LockFileTestThread thread : threads) {
			try {
				thread.join();
			} catch (final InterruptedException e) {
				doNothing();
			}
			if (thread.getError() != null)
				throw thread.getError();
		}
	}

	private static class LockFileTestThread extends Thread {
		private final int threadId;
		private final File file;
		private Exception error;

		public LockFileTestThread(final int threadId, final File file) {
			this.threadId = threadId;
			this.file = file;
		}

		@Override
		public void run() {
			try {
				for (int i = 0; i < 50; ++i) {
					acquireAndRelease(i);
				}
			} catch (final Exception x) {
				this.error = x;
				x.printStackTrace();
			}
		}

		private void acquireAndRelease(final int invocationId) {
			System.out.printf("[%s].acquireAndRelease[%s]: entered\n", threadId, invocationId);
			int time = random.nextInt(500);
			if (time > 0) {
				try { Thread.sleep(time); } catch (final InterruptedException e) { doNothing(); }
			}
			System.out.printf("[%s].acquireAndRelease[%s]: waited before lock\n", threadId, invocationId);
			try ( LockFile lockFile = LockFileFactory.getInstance().acquire(file, 10000); ) {
				System.out.printf("[%s].acquireAndRelease[%s]: locked\n", threadId, invocationId);
				time = random.nextInt(1000);
				if (time > 0) {
					try { Thread.sleep(time); } catch (final InterruptedException e) { doNothing(); }
				}
				System.out.printf("[%s].acquireAndRelease[%s]: waited before unlock\n", threadId, invocationId);
			}
			System.out.printf("[%s].acquireAndRelease[%s]: exiting\n", threadId, invocationId);
		}

		public Exception getError() {
			return error;
		}
	}
}
