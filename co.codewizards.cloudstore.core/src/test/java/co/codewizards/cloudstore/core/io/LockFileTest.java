package co.codewizards.cloudstore.core.io;

import java.io.File;

import org.junit.Test;

import co.codewizards.cloudstore.core.util.IOUtil;

public class LockFileTest {

	@Test
	public void acquireAndReleaseMultipleInstances() {
		File file = new File(IOUtil.getTempDir(), Long.toString(System.currentTimeMillis(), 36));
		LockFile lockFile1 = LockFileRegistry.getInstance().acquire(file, 10000);
		LockFile lockFile2 = LockFileRegistry.getInstance().acquire(file, 10000);
		try {
			try {
				System.out.println("Test");
			} finally {
				lockFile2.release();
			}
		} finally {
			lockFile1.release();
		}
	}

}
