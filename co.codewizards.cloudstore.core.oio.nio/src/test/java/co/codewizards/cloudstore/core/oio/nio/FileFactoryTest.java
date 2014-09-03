package co.codewizards.cloudstore.core.oio.nio;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.oio.FileFactory;
import co.codewizards.cloudstore.core.oio.IoFileFactory;
import co.codewizards.cloudstore.core.oio.nio.NioFileFactory;

@RunWith(value = Parameterized.class)
public class FileFactoryTest {

	public FileFactory fileFactory;

	public FileFactoryTest(final FileFactory fileFactory) {
		this.fileFactory = fileFactory;
	}

	@Test
	public final void createFile_string() {
		System.out.println("fileFactory=" + fileFactory.getClass().getSimpleName());
		final String fileName = "asdf";
		final File f = fileFactory.createFile(fileName);
		System.out.println("create file: " + f);

		assertThat(f.getAbsolutePath().contains(fileName)).isTrue();
	}

	@Test
	public final void createFile_file() {
		System.out.println("fileFactory=" + fileFactory.getClass().getSimpleName());
		final String fileName = "asdf";
		final java.io.File ioFile = new java.io.File("asdf");
		final File f = fileFactory.createFile(ioFile);
		System.out.println("created file: " + f);
		assertThat(f.getAbsolutePath().contains(fileName)).isTrue();
	}

	@Test
	public final void createFile_uri() throws URISyntaxException {
		System.out.println("fileFactory=" + fileFactory.getClass().getSimpleName());
		final String fileName = "asdf";
		final URI uri = new URI("file:/tmp/" + fileName);
		final File f = fileFactory.createFile(uri);
		System.out.println("created file: " + f);
		assertThat(f.getAbsolutePath().contains(fileName)).isTrue();
	}

	@Test
	public final void createFile_parentChild() {
		System.out.println("fileFactory=" + fileFactory.getClass().getSimpleName());
		final String parentName = "foo";
		final String childName = "bar";
		final File f = fileFactory.createFile(parentName, childName);
		System.out.println("created file: " + f);
		assertThat(f.getAbsolutePath().contains(parentName)).isTrue();
		assertThat(f.getAbsolutePath().contains(childName)).isTrue();
	}

	@Test
	public final void createFile_parentChildFile() {
		System.out.println("fileFactory=" + fileFactory.getClass().getSimpleName());
		final String parentName = "foo";
		final String childName = "bar";
		final File parentFile = fileFactory.createFile(parentName);
		final File f = fileFactory.createFile(parentFile, childName);
		System.out.println("created file: " + f);
		assertThat(f.getAbsolutePath().contains(parentName)).isTrue();
		assertThat(f.getAbsolutePath().contains(childName)).isTrue();
	}


	@Test
	public final void createTempDirectory() throws IOException {
		System.out.println("fileFactory=" + fileFactory.getClass().getSimpleName());
		final String prefix = "asdf";
		final File f = fileFactory.createTempDirectory(prefix);
		System.out.println("created file: " + f);
		assertThat(f.isDirectory()).isTrue();
		assertThat(f.getAbsolutePath().contains(prefix)).isTrue();
	}

	@Test
	public final void createTempFile() throws IOException {
		System.out.println("fileFactory=" + fileFactory.getClass().getSimpleName());
		final String prefix = "foo";
		final String suffix = "bar";
		final File f = fileFactory.createTempFile(prefix, suffix);
		System.out.println("created file: " + f);
		assertThat(f.isFile()).isTrue();
		assertThat(f.getAbsolutePath().contains(prefix)).isTrue();
		assertThat(f.getAbsolutePath().contains(suffix)).isTrue();
	}

	@Test
	public final void createTempFile_parentDir() throws IOException {
		System.out.println("fileFactory=" + fileFactory.getClass().getSimpleName());
		final String parentPrefix = "asdf";
		final File parentDir = fileFactory.createTempDirectory(parentPrefix);
		assertThat(parentDir.isDirectory()).isTrue();
		assertThat(parentDir.getAbsolutePath().contains(parentPrefix)).isTrue();

		final String prefix = "foo";
		final String suffix = "bar";
		final File f = fileFactory.createTempFile(prefix, suffix, parentDir);
		System.out.println("created file: " + f);
		assertThat(f.isFile()).isTrue();
		assertThat(f.getAbsolutePath().contains(prefix)).isTrue();
		assertThat(f.getAbsolutePath().contains(suffix)).isTrue();
	}


	@Parameterized.Parameters
	public static Collection<Object[]> instancesToTest() {
		return Arrays.asList(
				new Object[] { new NioFileFactory() },
				new Object[] { new IoFileFactory() }
		);
	}

}
