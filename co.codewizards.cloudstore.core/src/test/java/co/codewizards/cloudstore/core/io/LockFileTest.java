package co.codewizards.cloudstore.core.io;

import java.io.File;

import org.junit.Test;

import co.codewizards.cloudstore.core.util.IOUtil;

public class LockFileTest {

	@Test
	public void acquireAndReleaseMultipleInstances() {
		final File file = new File(IOUtil.getTempDir(), Long.toString(System.currentTimeMillis(), 36));
		try ( LockFile lockFile1 = LockFileFactory.getInstance().acquire(file, 10000); ) {
			try ( LockFile lockFile2 = LockFileFactory.getInstance().acquire(file, 10000); ) {
				System.out.println("Test");
			}
		}
	}

}
