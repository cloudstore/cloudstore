/**
 *
 */
package co.codewizards.cloudstore.core.util;

import static co.codewizards.cloudstore.core.oio.file.FileFactory.*;
import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

import co.codewizards.cloudstore.core.oio.file.File;

/**
 * @author Sebastian Schefczyk
 */
public class IOUtilTest {

	@Test
	public void testInTmp() throws IOException {
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
