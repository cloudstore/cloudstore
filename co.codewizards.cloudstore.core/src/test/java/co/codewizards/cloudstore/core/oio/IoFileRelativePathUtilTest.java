package co.codewizards.cloudstore.core.oio;

import static java.util.Objects.*;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

/**
 * @author Sebastian Schefczyk
 */
public class IoFileRelativePathUtilTest {

    private final static String STEP_UP = "../";

    @Test
    public void testInTargetDir() throws IOException {
        final File testDir = new File(new File("target"), "testDir");
        testDir.mkdir();

        final File subFolder = new File(testDir, "subFolder");
        final File fileName = new File(subFolder, "fileName");

        final String relPath = IoFileRelativePathUtil.getRelativePath(testDir, fileName);

        requireNonNull(relPath);
        assertTrue(fileName.getAbsolutePath().endsWith(relPath));
        assertEquals("subFolder/fileName", relPath);
    }

    @Test
    public void testInTmp() throws IOException {
        final File testDir = new File(new File("/tmp/IOUtilTest"), "testDir");
        testDir.mkdir();

        final File subFolder = new File(testDir, "subFolder");
        final File fileName = new File(subFolder, "fileName");

        final String relPath = IoFileRelativePathUtil.getRelativePath(testDir, fileName);

        requireNonNull(relPath);
        assertTrue(fileName.getAbsolutePath().endsWith(relPath));
        assertEquals("subFolder/fileName", relPath);
    }


    @Test
    public void stepIntoSubfolder() throws IOException {
        final String baseDir = "/tmp/folder/";
        final String testFile1 = "/tmp/folder/subFolder1/fileName1";

        final String relPath = IoFileRelativePathUtil.getRelativePath(baseDir, testFile1, true, File.separatorChar);

        requireNonNull(relPath);
        assertEquals("subFolder1/fileName1", relPath);
    }

    @Test
    public void stepUpOneFolder() throws IOException {
        final String baseDir = "/tmp/folder/subFolder";
        final String testFile1 = "/tmp/folder/subFolder1/fileName1";

        final String relPath = IoFileRelativePathUtil.getRelativePath(baseDir, testFile1, true, File.separatorChar);

        requireNonNull(relPath);
        assertEquals(STEP_UP + "subFolder1/fileName1", relPath);
    }

    @Test
    public void parallelToFileSubfolder() throws IOException {
        final String baseDir = "/tmp/folder/file";
        final String testFile1 = "/tmp/folder/subFolder1/fileName";

        final String relPath = IoFileRelativePathUtil.getRelativePath(baseDir, testFile1, false, File.separatorChar);

        requireNonNull(relPath);
//        assertEquals("subFolder1/fileName", relPath);
        assertEquals("../subFolder1/fileName", relPath); // like java.nio.Path.relativize
    }

    @Test
    public void parallelToFile_stepUp() throws IOException {
        final String baseDir =   "/tmp/folder/subfolder/file";
        final String testFile1 = "/tmp/folder/subFolder1/fileName1";

        final String relPath = IoFileRelativePathUtil.getRelativePath(baseDir, testFile1, false, File.separatorChar);

        requireNonNull(relPath);
//        assertEquals("../subFolder1/fileName1", relPath);
        assertEquals("../../subFolder1/fileName1", relPath); // like java.nio.Path.relativize
    }



}