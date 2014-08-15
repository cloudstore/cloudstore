/**
 *
 */
package co.codewizards.cloudstore.core.util;

import static java.lang.System.*;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
		final File testDir = new File(new File("/tmp/IOUtilTest"), "testDir");
		testDir.mkdirs();
		System.out.println("testDir=  " + testDir.getAbsolutePath());

		final File subFolder = new File(testDir, "subFolder");
		final File fileName = new File(subFolder, "fileName");
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
		final File tmpDir = Files.createTempDirectory(this.getClass().getSimpleName()).toFile();
		final File testDir = tmpDir;
		System.out.println("testDir=  " + testDir.getAbsolutePath());

		final File subFolder = new File(testDir, "subFolder");
		subFolder.mkdirs();
		final File fileName = new File(subFolder, "fileName");
		fileName.createNewFile();
		System.out.println("fileName= " + fileName.getAbsolutePath());

		final String relPath = IOUtil.getRelativePath(testDir, fileName);

		System.out.println("relPath= " + relPath);
		assertNotNull(relPath);
		assertTrue(fileName.getAbsolutePath().endsWith(relPath));

		assertEquals("subFolder/fileName", relPath);
	}

}
