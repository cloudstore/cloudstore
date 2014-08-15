/**
 *
 */
package co.codewizards.cloudstore.core.util;

import static co.codewizards.cloudstore.core.oio.file.FileFactory.*;
import static java.lang.System.*;
import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.oio.file.File;

/**
 * @author Sebastian Schefczyk
 */
public class IOUtilTest {

	private static final Logger logger = LoggerFactory.getLogger(IOUtilTest.class);

	{
		logger.debug("[{}]<init>", Integer.toHexString(identityHashCode(this)));
	}

	@Test
	public void testInTmp() throws IOException {
		logger.debug("[{}]testInTmp: entered.", Integer.toHexString(identityHashCode(this)));
		final File testDir = newFile(newFile("/tmp/IOUtilTest"), "testDir");
		testDir.mkdirs();
		System.out.println("testDir=  " + testDir.getAbsolutePath());

		final File subFolder = newFile(testDir, "subFolder");
		final File fileName = newFile(subFolder, "fileName");
		System.out.println("fileName= " + fileName.getAbsolutePath());

		final String relPath = IOUtil.getRelativePath(testDir, fileName);

		System.out.println("relPath= " + relPath);
		assertNotNull(relPath);
		assertTrue(fileName.getAbsolutePath().endsWith(relPath));

		assertEquals("subFolder/fileName", relPath);
	}

	@Test
	public void testInTargetDir() throws IOException {
		logger.debug("[{}]testInTargetDir: entered.", Integer.toHexString(identityHashCode(this)));
		System.out.println("#######   testInTargetDir   #######");
		final File tmpDir = createTempDirectory(this.getClass().getSimpleName());
		final File testDir = tmpDir;
		System.out.println("testDir=  " + testDir.getAbsolutePath());

		final File subFolder = newFile(testDir, "subFolder");
		subFolder.mkdirs();
		final File fileName = newFile(subFolder, "fileName");
		fileName.createNewFile();
		System.out.println("fileName= " + fileName.getAbsolutePath());

		final String relPath = IOUtil.getRelativePath(testDir, fileName);

		System.out.println("relPath= " + relPath);
		assertNotNull(relPath);
		assertTrue(fileName.getAbsolutePath().endsWith(relPath));

		assertEquals("subFolder/fileName", relPath);
	}

}
