package co.codewizards.cloudstore.oio.test;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;

import org.junit.Test;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.oio.io.IoFileFactory;
import co.codewizards.cloudstore.oio.io.IoFileUtil;


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

		final File newFileName = IoFileUtil.newFileName(fromDir, toDir, current);
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

		final File newFileName = IoFileUtil.newFileName(fromDir, toDir, subDir);
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

		final File newFileName = IoFileUtil.newFileName(fromDir, toDir, current);

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
		final File current = ioFileFactory.createFile(fromDirSubDir, "a");
		current.createNewFile();

		IoFileUtil.deleteRecursively(fromDir.getIoFile());

		assertThat(fromDirSubDir.exists()).isFalse();
		assertThat(fromDirFile.exists()).isFalse();
		assertThat(current.exists()).isFalse();
		assertThat(fromDir.exists()).isFalse();
	}


}
