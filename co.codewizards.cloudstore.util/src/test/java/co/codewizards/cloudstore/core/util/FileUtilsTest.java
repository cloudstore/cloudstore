
package co.codewizards.cloudstore.core.util;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import co.codewizards.cloudstore.util.FileUtils;

/**
 * @author Sebastian Schefczyk
 */
public class FileUtilsTest {

    private final static String STEP_UP = "../";

    @Test
    public void testInTargetDir() throws IOException {
        final File testDir = new File(new File("target"), "testDir");
        testDir.mkdir();

        final File subFolder = new File(testDir, "subFolder");
        final File fileName = new File(subFolder, "fileName");

        final String relPath = FileUtils.getRelativePath(testDir, fileName);

        assertNotNull(relPath);
        assertTrue(fileName.getAbsolutePath().endsWith(relPath));
        assertEquals("subFolder/fileName", relPath);
    }

    @Test
    public void testInTmp() throws IOException {
        final File testDir = new File(new File("/tmp/IOUtilTest"), "testDir");
        testDir.mkdir();

        final File subFolder = new File(testDir, "subFolder");
        final File fileName = new File(subFolder, "fileName");

        final String relPath = FileUtils.getRelativePath(testDir, fileName);

        assertNotNull(relPath);
        assertTrue(fileName.getAbsolutePath().endsWith(relPath));
        assertEquals("subFolder/fileName", relPath);
    }


    @Test
    public void stepIntoSubfolder() throws IOException {
        final String baseDir = "/tmp/folder/";
        final String testFile1 = "/tmp/folder/subFolder1/fileName1";

        final String relPath = FileUtils.getRelativePath(baseDir, testFile1, true, File.separatorChar);

        assertNotNull(relPath);
        assertEquals("subFolder1/fileName1", relPath);
    }

    @Test
    public void stepUpOneFolder() throws IOException {
        final String baseDir = "/tmp/folder/subFolder";
        final String testFile1 = "/tmp/folder/subFolder1/fileName1";

        final String relPath = FileUtils.getRelativePath(baseDir, testFile1, true, File.separatorChar);

        assertNotNull(relPath);
        assertEquals(STEP_UP + "subFolder1/fileName1", relPath);
    }

    @Test
    public void parallelToFileSubfolder() throws IOException {
        final String baseDir = "/tmp/folder/file";
        final String testFile1 = "/tmp/folder/subFolder1/fileName";

        final String relPath = FileUtils.getRelativePath(baseDir, testFile1, false, File.separatorChar);

        assertNotNull(relPath);
//        assertEquals("subFolder1/fileName", relPath);
        assertEquals("../subFolder1/fileName", relPath); // like java.nio.Path.relativize
    }

    @Test
    public void parallelToFile_stepUp() throws IOException {
        final String baseDir =   "/tmp/folder/subfolder/file";
        final String testFile1 = "/tmp/folder/subFolder1/fileName1";

        final String relPath = FileUtils.getRelativePath(baseDir, testFile1, false, File.separatorChar);

        assertNotNull(relPath);
//        assertEquals("../subFolder1/fileName1", relPath);
        assertEquals("../../subFolder1/fileName1", relPath); // like java.nio.Path.relativize
    }



}