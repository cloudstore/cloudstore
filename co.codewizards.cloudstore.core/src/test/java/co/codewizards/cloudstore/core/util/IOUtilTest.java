package co.codewizards.cloudstore.core.util;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.IOUtil.*;
import static java.lang.System.*;
import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.oio.File;

/**
 * @author Sebastian Schefczyk
 */
public class IOUtilTest {

	private static final Logger logger = LoggerFactory.getLogger(IOUtilTest.class);

	private static Random random = new Random();

	{
		logger.debug("[{}]<init>", Integer.toHexString(identityHashCode(this)));
	}

	@Test
	public void testInTmp() throws IOException {
		logger.debug("[{}]testInTmp: entered.", Integer.toHexString(identityHashCode(this)));
		final File testDir = createFile(createFile("/tmp/IOUtilTest"), "testDir");
		testDir.mkdirs();
		System.out.println("testDir=  " + testDir.getAbsolutePath());

		final File subFolder = createFile(testDir, "subFolder");
		final File fileName = createFile(subFolder, "fileName");
		System.out.println("fileName= " + fileName.getAbsolutePath());

		final String relPath = getRelativePath(testDir, fileName);

		System.out.println("relPath= " + relPath);
		assertThat(relPath).isNotNull();
		assertThat(fileName.getAbsolutePath().endsWith(relPath)).isTrue();

		assertThat(relPath).isEqualTo("subFolder/fileName");
	}

	@Test
	public void testInTargetDir() throws IOException {
		logger.debug("[{}]testInTargetDir: entered.", Integer.toHexString(identityHashCode(this)));
		System.out.println("#######   testInTargetDir   #######");
		final File tmpDir = createTempDirectory(this.getClass().getSimpleName());
		final File testDir = tmpDir;
		System.out.println("testDir=  " + testDir.getAbsolutePath());

		final File subFolder = createFile(testDir, "subFolder");
		subFolder.mkdirs();
		final File fileName = createFile(subFolder, "fileName");

		fileName.createNewFile();
		System.out.println("fileName= " + fileName.getAbsolutePath());

		final String relPath = getRelativePath(testDir, fileName);

		System.out.println("relPath= " + relPath);
		assertThat(relPath).isNotNull();
		assertThat(fileName.getAbsolutePath().endsWith(relPath)).isTrue();

		assertThat(relPath).isEqualTo("subFolder/fileName");
	}

	@Test
	public void bytesToLongToBytes() {
		final byte[] bytes = longToBytes(Long.MAX_VALUE);
		long l = bytesToLong(bytes);
		assertThat(l).isEqualTo(Long.MAX_VALUE);

		for (int i = 0; i < 100; ++i) {
			random.nextBytes(bytes);
			l = bytesToLong(bytes);
			final byte[] bytes2 = longToBytes(l);
			assertThat(bytes2).isEqualTo(bytes);
		}
	}

	@Test
	public void bytesToIntToBytes() {
		final byte[] bytes = intToBytes(Integer.MAX_VALUE);
		int l = bytesToInt(bytes);
		assertThat(l).isEqualTo(Integer.MAX_VALUE);

		for (int i = 0; i < 100; ++i) {
			random.nextBytes(bytes);
			l = bytesToInt(bytes);
			final byte[] bytes2 = intToBytes(l);
			assertThat(bytes2).isEqualTo(bytes);
		}
	}

	@Test
	public void replaceTemplateVariables_nested() {
		Map<String, Object> variables = new HashMap<>();
		variables.put("appId.simpleId", "blabla");

		String template = "trallalitrallala = ${${appId.simpleId}.xxx}.oink";
		String resolved = IOUtil.replaceTemplateVariables(template, variables);

		assertThat(resolved).isEqualTo("trallalitrallala = ${blabla.xxx}.oink");
	}

}
