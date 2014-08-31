package co.codewizards.cloudstore.core.oio.nio;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;

import org.junit.Test;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.oio.IoFileFactory;
import co.codewizards.cloudstore.core.oio.IoFileUtil;
import co.codewizards.cloudstore.core.oio.nio.NioFileFactory;


/**
 * @author Sebastian Schefczyk
 *
 */
public class IoFileUtilTest {

	@Test
	public void newFileName_oneFile() throws IOException {
		final IoFileFactory ioFileFactory = new IoFileFactory();

		final File fromDir = ioFileFactory.createTempDirectory("from");
		final File toDir = ioFileFactory.createTempDirectory("to");
		final File current = ioFileFactory.createFile(fromDir, "a");

		System.out.println("### newFileName_oneFile ###");
		System.out.println("fromDir: " + fromDir);
		System.out.println("toDir: " + toDir);
		System.out.println("current: " + current);

		final File newFileName = IoFileUtil.newFileNameForRenameTo(fromDir, toDir, current);
		final String expected = toDir.getAbsolutePath() + java.io.File.separator + "a";
		assertThat(newFileName.getAbsolutePath()).isEqualTo(expected);
	}

	@Test
	public void newFileName_subdir() throws IOException {
		final IoFileFactory ioFileFactory = new IoFileFactory();

		final File fromDir = ioFileFactory.createTempDirectory("from0-");
		final File toDir = ioFileFactory.createTempDirectory("to0-");
		final File subDir = ioFileFactory.createFile(fromDir, "s");

		System.out.println("### newFileName_subdir ###");
		System.out.println("fromDir: " + fromDir);
		System.out.println("toDir: " + toDir);
		System.out.println("subDir: " + subDir);

		final File newFileName = IoFileUtil.newFileNameForRenameTo(fromDir, toDir, subDir);
		final String expected = toDir.getAbsolutePath() + java.io.File.separator + "s";
		assertThat(newFileName.getAbsolutePath()).isEqualTo(expected);
	}

	@Test
	public void newFileName_subdirFile() throws IOException {
		final IoFileFactory ioFileFactory = new IoFileFactory();

		final File fromDir = ioFileFactory.createTempDirectory("from1-");
		final File toDir = ioFileFactory.createTempDirectory("to1-");
		final File subDir = ioFileFactory.createFile(fromDir, "s");
		final File current = ioFileFactory.createFile(subDir, "a");

//		System.out.println("### newFileName_subdirFile ###");
//		System.out.println("fromDir: " + fromDir);
//		System.out.println("toDir: " + toDir);
//		System.out.println("subDir: " + subDir);
//		System.out.println("current: " + current);

		final File newFileName = IoFileUtil.newFileNameForRenameTo(fromDir, toDir, current);

		final String expected = toDir.getAbsolutePath() + java.io.File.separator + "s" + java.io.File.separator + "a";
		assertThat(newFileName.getAbsolutePath()).isEqualTo(expected);
	}


	@Test
	public void moveRecursively() throws IOException {
		final IoFileFactory ioFileFactory = new IoFileFactory();

		final File fromDir = ioFileFactory.createTempDirectory("from2-");
		final File toDir = ioFileFactory.createTempDirectory("to2-");
		final File fromDirSubDir = ioFileFactory.createFile(fromDir, "s");
		fromDirSubDir.mkdir();
		final File fromDirFile = ioFileFactory.createFile(fromDir, "f");
		fromDirFile.createNewFile();
		final File current = ioFileFactory.createFile(fromDirSubDir, "a");
		current.createNewFile();

		IoFileUtil.moveRecursively(fromDir, toDir);

		final String expectedToDir = toDir.getAbsolutePath();
		final String expectedSubDir = toDir.getAbsolutePath() + java.io.File.separator + "s";
		final String expectedFile = toDir.getAbsolutePath() + java.io.File.separator + "f";
		final String expectedSubDirChild = toDir.getAbsolutePath() + java.io.File.separator + "s" + java.io.File.separator + "a";
		assertThat(fromDirSubDir.exists()).isFalse();
		assertThat(fromDirFile.exists()).isFalse();
		assertThat(current.exists()).isFalse();
		assertThat(fromDir.exists()).isFalse();
		assertThat(ioFileFactory.createFile(expectedToDir).exists()).isTrue();
		assertThat(ioFileFactory.createFile(expectedSubDir).exists()).isTrue();
		assertThat(ioFileFactory.createFile(expectedSubDir).isDirectory()).isTrue();
		assertThat(ioFileFactory.createFile(expectedFile).exists()).isTrue();
		assertThat(ioFileFactory.createFile(expectedFile).isFile()).isTrue();
		assertThat(ioFileFactory.createFile(expectedSubDirChild).exists()).isTrue();
		assertThat(ioFileFactory.createFile(expectedSubDirChild).isFile()).isTrue();
	}


	@Test
	public void deleteRecursively() throws IOException {
		final IoFileFactory ioFileFactory = new IoFileFactory();

		final File fromDir = ioFileFactory.createTempDirectory("from2-");
		final File fromDirSubDir = ioFileFactory.createFile(fromDir, "s");
		fromDirSubDir.mkdir();
		final File fromDirFile = ioFileFactory.createFile(fromDir, "f");
		fromDirFile.createNewFile();
		final File fromDirSubDirChildFile = ioFileFactory.createFile(fromDirSubDir, "a");
		fromDirSubDirChildFile.createNewFile();
		final File fromDirSubDirEmpty = ioFileFactory.createFile(fromDir, "e");
		fromDirSubDirEmpty.mkdir();

		fromDir.deleteRecursively();

		assertThat(fromDirSubDir.exists()).isFalse();
		assertThat(fromDirFile.exists()).isFalse();
		assertThat(fromDirSubDirChildFile.exists()).isFalse();
		assertThat(fromDirSubDirEmpty.exists()).isFalse();
		assertThat(fromDir.exists()).isFalse();
	}

	/** This test must support symlinks! */
	@Test
	public void deleteRecursively_noFollowSymLinks() throws IOException {
		final IoFileFactory ioFileFactory = new IoFileFactory();

		final File fromDir = ioFileFactory.createTempDirectory("from3-");
		final File fromDirSubDir = ioFileFactory.createFile(fromDir, "s");
		fromDirSubDir.mkdir();
		final File fromDirFile = ioFileFactory.createFile(fromDir, "f");
		fromDirFile.createNewFile();
		final File current = ioFileFactory.createFile(fromDirSubDir, "cf");
		current.createNewFile();

		//2nd directory, which gets linked and should not be deleted
		final File dirTwo = ioFileFactory.createTempDirectory("dirTwo-");
		final File dirTwoSubDir = ioFileFactory.createFile(dirTwo, "ss");
		dirTwoSubDir.mkdir();
		final File dirTwoFile = ioFileFactory.createFile(dirTwo, "ff");
		dirTwoFile.createNewFile();
		final File dirTwoChildFile = ioFileFactory.createFile(dirTwoSubDir, "cfcf");
		dirTwoChildFile.createNewFile();

		// to create a symlink, we have to take the NioFileFactory
		final NioFileFactory nioFileFactory = new NioFileFactory();
		// create the symlink to dirTwo:
		final File symlinkTwo = nioFileFactory.createFile(fromDirSubDir.getAbsolutePath(), "symlinkToDirTwo");
		symlinkTwo.createSymbolicLink(dirTwo.getAbsolutePath());
		assertThat(symlinkTwo.exists()).isTrue();
		assertThat(symlinkTwo.isSymbolicLink()).isTrue();
		assertThat(symlinkTwo.canWrite()).isTrue();

		fromDir.deleteRecursively();
//		IOUtil.deleteDirectoryRecursively(fromDir);

		// same as in the other test
		assertThat(fromDirSubDir.exists()).isFalse();
		assertThat(fromDirFile.exists()).isFalse();
		assertThat(current.exists()).isFalse();
		assertThat(fromDir.exists()).isFalse();
		// check the symlink got deleted itself:
		assertThat(symlinkTwo.exists()).isFalse();
		// check the contents of the dirTwo directory have survived
		assertThat(dirTwo.exists()).isTrue();
		assertThat(dirTwoSubDir.exists()).isTrue();
		assertThat(dirTwoFile.exists()).isTrue();
		assertThat(dirTwoChildFile.exists()).isTrue();
	}

//	/** This test must support symlinks! */
////	@Ignore("Move to IOUtilsTest, assert factories")
//	@Test
//	public void deleteRecursively_meanSymLink() throws IOException {
//		final IoFileFactory ioFileFactory = new IoFileFactory();
//
//		final File fromDir = ioFileFactory.createTempDirectory("from3-");
//		final File fromDirSubDir = ioFileFactory.createFile(fromDir, "s");
//		fromDirSubDir.mkdir();
//		final File fromDirFile = ioFileFactory.createFile(fromDir, "f");
//		fromDirFile.createNewFile();
//		final File fromDirChildFile = ioFileFactory.createFile(fromDirSubDir, "cf");
//		fromDirChildFile.createNewFile();
//
//		//3nd directory, which gets linked and should not be deleted; but symlink was set to noWrite
//		final File dirMean = ioFileFactory.createTempDirectory("dirMean-");
//		final dirMean.changeOwnership //yeah, needed for testing
//		final File dirMeanSubDir = ioFileFactory.createFile(dirMean, "ss");
//		dirMeanSubDir.mkdir();
//		final File dirMeanFile = ioFileFactory.createFile(dirMean, "ff");
//		dirMeanFile.createNewFile();
//		final File dirMeanChildFile = ioFileFactory.createFile(dirMeanSubDir, "cfcf");
//		dirMeanChildFile.createNewFile();
//
//		// to create a symlink, we have to take the NioFileFactory
//		final NioFileFactory nioFileFactory = new NioFileFactory();
//
//		// create the symlink to dirTwo:
//		final File symlinkMean = nioFileFactory.createFile(fromDirSubDir.getAbsolutePath(), "symlinkToDirMean");
//		symlinkMean.createSymbolicLink(dirMean.getAbsolutePath());
//		assertThat(symlinkMean.exists()).isTrue();
//		assertThat(symlinkMean.isSymbolicLink()).isTrue();
//		symlinkMean.deleteOnExit(); //perhaps this will even work after set unwritable ;-)
//
////		IOUtil.deleteDirectoryRecursively(fromDir);
//		fromDir.deleteRecursively();
//
//		//unwritable symlinkMean is here: ./fromDir/fromDirSubDir/symlinkMean
//		assertThat(fromDirSubDir.exists()).isTrue();
//		assertThat(fromDirFile.exists()).isFalse();
//		assertThat(fromDirChildFile.exists()).isFalse();
//		assertThat(fromDir.exists()).isTrue();
//		// check the symlink got deleted itself:
//		assertThat(symlinkMean.exists()).isTrue();
//		// check the contents of the dirMean directory have survived
//		assertThat(dirMean.exists()).isTrue();
//		assertThat(dirMeanSubDir.exists()).isTrue();
//		assertThat(dirMeanFile.exists()).isTrue();
//		assertThat(dirMeanChildFile.exists()).isTrue();
//	}
}
