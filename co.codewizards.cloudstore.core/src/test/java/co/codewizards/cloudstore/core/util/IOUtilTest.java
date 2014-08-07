/**
 *
 */
package co.codewizards.cloudstore.core.util;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.Test;

/**
 * @author Sebastian Schefczyk
 */
public class IOUtilTest {

	@Test
	public void testInTmp() throws IOException {
		File testDir = new File(new File("/tmp/IOUtilTest"), "testDir");
		testDir.mkdirs();
		System.out.println("testDir=  " + testDir.getAbsolutePath());

		File subFolder = new File(testDir, "subFolder");
		File fileName = new File(subFolder, "fileName");
		System.out.println("fileName= " + fileName.getAbsolutePath());

		String relPath = IOUtil.getRelativePath(testDir, fileName);

		System.out.println("relPath= " + relPath);
		assertNotNull(relPath);
		assertTrue(fileName.getAbsolutePath().endsWith(relPath));

		assertEquals("subFolder/fileName", relPath);
	}

	@Test
	public void testInTargetDir() throws IOException {
		System.out.println("#######   testInTargetDir   #######");
		File tmpDir = Files.createTempDirectory(this.getClass().getSimpleName()).toFile();
		File testDir = tmpDir;
		System.out.println("testDir=  " + testDir.getAbsolutePath());

		File subFolder = new File(testDir, "subFolder");
		subFolder.mkdirs();
		File fileName = new File(subFolder, "fileName");
		fileName.createNewFile();
		System.out.println("fileName= " + fileName.getAbsolutePath());

		String relPath = IOUtil.getRelativePath(testDir, fileName);

		System.out.println("relPath= " + relPath);
		assertNotNull(relPath);
		assertTrue(fileName.getAbsolutePath().endsWith(relPath));

		assertEquals("subFolder/fileName", relPath);
	}

}
