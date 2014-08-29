package co.codewizards.cloudstore.core.config;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.Util.*;
import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import org.junit.Test;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerFactory;
import co.codewizards.cloudstore.core.repo.transport.FileWriteStrategy;
import co.codewizards.cloudstore.local.AbstractTest;

public class ConfigTest extends AbstractTest {

	@Override
	public void after() {
		for (final File file : Config.getInstance().propertiesFiles) {
			file.delete();
			assertThat(file.exists()).isFalse();
		}
	}

	/**
	 * Tests whether the global configuration file is named as documented on the
	 * web-site. If this test breaks, it must be verified, if the name really needs to be changed
	 * and if so, the documentation must be updated together with this test!!!
	 */
	@Test
	public void testGlobalConfigFileName() throws Exception {
		final Config globalConfig = Config.getInstance();
		assertThat(globalConfig.propertiesFiles.length).isEqualTo(1);
		assertThat(globalConfig.propertiesFiles[0]).isNotNull();
		assertThat(globalConfig.propertiesFiles[0].getName()).isEqualTo("cloudstore.properties");
	}

	@Test
	public void testGlobalConfigFileModification() throws Exception {
		final Config globalConfig = Config.getInstance();
		final String testKey = "testKey0";
		String value = globalConfig.getProperty(testKey, null);
		assertThat(value).isNull();

		waitForDifferentLastModifiedTimestamp();

		setGlobalProperty(testKey, "testValueAAA");
		value = globalConfig.getProperty(testKey, null);
		assertThat(value).isEqualTo("testValueAAA");

		waitForDifferentLastModifiedTimestamp();

		setGlobalProperty(testKey, "testValueBBB");
		value = globalConfig.getProperty(testKey, null);
		assertThat(value).isEqualTo("testValueBBB");
	}

	@Test
	public void testConfigInheritance() throws Exception {
		final String testKey1 = "testKey1";
		final String testKey2 = "testKey2";

		final File localRoot = newTestRepositoryLocalRoot();
		assertThat(localRoot.exists()).isFalse();
		localRoot.mkdirs();
		assertThat(localRoot.isDirectory()).isTrue();

		final LocalRepoManager localRepoManager = LocalRepoManagerFactory.Helper.getInstance().createLocalRepoManagerForNewRepository(localRoot);
		assertThat(localRepoManager).isNotNull();

		final File child_1 = createFile(localRoot, "1");
		assertThat(child_1.exists()).isFalse();
		final Config config_1 = Config.getInstanceForDirectory(child_1);
		assertThat(config_1.getPropertyAsNonEmptyTrimmedString(testKey1, null)).isNull();
		createDirectory(child_1);
		assertThat(child_1.isDirectory()).isTrue();
		setProperty(createFile(child_1, ".cloudstore.properties"), testKey1, "   testValueAAA     ");
		assertThat(config_1.getPropertyAsNonEmptyTrimmedString(testKey1, null)).isEqualTo("testValueAAA");
		setProperty(createFile(child_1, "cloudstore.properties"), testKey1, "    testValueBBB  ");
		assertThat(config_1.getPropertyAsNonEmptyTrimmedString(testKey1, null)).isEqualTo("testValueBBB");

		assertThat(config_1.getPropertyAsEnum(FileWriteStrategy.CONFIG_KEY, FileWriteStrategy.directAfterTransfer)).isEqualTo(FileWriteStrategy.directAfterTransfer);
		assertThat(config_1.getPropertyAsEnum(FileWriteStrategy.CONFIG_KEY, FileWriteStrategy.class, null)).isNull();

		waitForDifferentLastModifiedTimestamp();
		setGlobalProperty(FileWriteStrategy.CONFIG_KEY, FileWriteStrategy.replaceAfterTransfer.name());
		assertThat(config_1.getPropertyAsEnum(FileWriteStrategy.CONFIG_KEY, FileWriteStrategy.directAfterTransfer)).isEqualTo(FileWriteStrategy.replaceAfterTransfer);

		final File child_1_a = createFile(child_1, "a");
		final Config config_1_a = Config.getInstanceForFile(child_1_a);

		assertThat(config_1_a.getPropertyAsEnum(FileWriteStrategy.CONFIG_KEY, FileWriteStrategy.directAfterTransfer)).isEqualTo(FileWriteStrategy.replaceAfterTransfer);
		assertThat(config_1_a.getPropertyAsNonEmptyTrimmedString(testKey1, null)).isEqualTo("testValueBBB");

		waitForDifferentLastModifiedTimestamp();

		setProperty(createFile(child_1, ".a.cloudstore.properties"), testKey1, "testValueCCC");
		setProperty(createFile(child_1, ".a.cloudstore.properties"), FileWriteStrategy.CONFIG_KEY, FileWriteStrategy.directDuringTransfer.name());

		assertThat(config_1_a.getPropertyAsNonEmptyTrimmedString(testKey1, null)).isEqualTo("testValueCCC");
		assertThat(config_1_a.getPropertyAsEnum(FileWriteStrategy.CONFIG_KEY, FileWriteStrategy.directAfterTransfer)).isEqualTo(FileWriteStrategy.directDuringTransfer);

		setProperty(createFile(child_1, "a.cloudstore.properties"), testKey1, "   testValueDDD  ");
		assertThat(config_1_a.getPropertyAsNonEmptyTrimmedString(testKey1, null)).isEqualTo("testValueDDD");

		waitForDifferentLastModifiedTimestamp();

		setProperty(createFile(child_1, "a.cloudstore.properties"), testKey1, "    ");
		assertThat(config_1_a.getPropertyAsNonEmptyTrimmedString(testKey1, "xxxyyyzzz")).isEqualTo("xxxyyyzzz");
		assertThat(config_1_a.getProperty(testKey1, "xxxyyyzzz")).isEqualTo("    ");

		waitForDifferentLastModifiedTimestamp();

		setProperty(createFile(child_1, "a.cloudstore.properties"), testKey1, null);
		assertThat(config_1_a.getPropertyAsNonEmptyTrimmedString(testKey1, "xxxyyyzzz")).isEqualTo("testValueCCC");

		createFileWithRandomContent(child_1_a);

		final File child_1_2 = createDirectory(child_1, "2");
		final File child_1_2_aaa = createFile(child_1_2, "aaa");
		createFileWithRandomContent(child_1_2_aaa);

		final Config config_1_2_aaa = Config.getInstanceForFile(child_1_2_aaa);
		assertThat(config_1_2_aaa.getPropertyAsNonEmptyTrimmedString(testKey1, "xxxyyyzzz")).isEqualTo("testValueBBB");

		setProperty(createFile(child_1_2_aaa.getParentFile(), ".cloudstore.properties"), testKey1, "val_1_2_hidden");
		assertThat(config_1_2_aaa.getProperty(testKey1, null)).isEqualTo("val_1_2_hidden");

		setProperty(createFile(child_1_2_aaa.getParentFile(), "cloudstore.properties"), testKey1, "val_1_2_visible");
		assertThat(config_1_2_aaa.getProperty(testKey1, null)).isEqualTo("val_1_2_visible");

		assertThat(config_1_2_aaa.getPropertyAsEnum(FileWriteStrategy.CONFIG_KEY, FileWriteStrategy.directAfterTransfer)).isEqualTo(FileWriteStrategy.replaceAfterTransfer); // global
		setProperty(createFile(localRoot, "cloudstore.properties"), FileWriteStrategy.CONFIG_KEY, FileWriteStrategy.directDuringTransfer.name());
		assertThat(config_1_2_aaa.getPropertyAsEnum(FileWriteStrategy.CONFIG_KEY, FileWriteStrategy.directAfterTransfer)).isEqualTo(FileWriteStrategy.directDuringTransfer); // root directory

		setProperty(createFile(child_1_2_aaa.getParentFile(), "aaa.cloudstore.properties"), testKey1, "val_1_2_aaa_visible");
		assertThat(config_1_2_aaa.getProperty(testKey1, null)).isEqualTo("val_1_2_aaa_visible");

		waitForDifferentLastModifiedTimestamp();
		setProperty(createFile(child_1_2_aaa.getParentFile(), ".cloudstore.properties"), FileWriteStrategy.CONFIG_KEY, FileWriteStrategy.directAfterTransfer.name());
		assertThat(config_1_2_aaa.getPropertyAsEnum(FileWriteStrategy.CONFIG_KEY, FileWriteStrategy.directAfterTransfer)).isEqualTo(FileWriteStrategy.directAfterTransfer);

		waitForDifferentLastModifiedTimestamp();
		setProperty(createFile(child_1_2_aaa.getParentFile(), ".cloudstore.properties"), FileWriteStrategy.CONFIG_KEY, "");
		assertThat(config_1_2_aaa.getPropertyAsEnum(FileWriteStrategy.CONFIG_KEY, FileWriteStrategy.class, null)).isNull();

		waitForDifferentLastModifiedTimestamp();

		setProperty(createFile(child_1_2_aaa.getParentFile(), "aaa.cloudstore.properties"), FileWriteStrategy.CONFIG_KEY, FileWriteStrategy.directDuringTransfer.name());
		assertThat(config_1_2_aaa.getPropertyAsEnum(FileWriteStrategy.CONFIG_KEY, FileWriteStrategy.class, null)).isEqualTo(FileWriteStrategy.directDuringTransfer);

		waitForDifferentLastModifiedTimestamp();

		setProperty(createFile(child_1_2_aaa.getParentFile(), "aaa.cloudstore.properties"), FileWriteStrategy.CONFIG_KEY, null);
		assertThat(config_1_2_aaa.getPropertyAsEnum(FileWriteStrategy.CONFIG_KEY, FileWriteStrategy.class, null)).isNull();

		setProperty(createFile(child_1_2_aaa.getParentFile(), ".cloudstore.properties"), FileWriteStrategy.CONFIG_KEY, null);
		assertThat(config_1_2_aaa.getPropertyAsEnum(FileWriteStrategy.CONFIG_KEY, FileWriteStrategy.class, null)).isEqualTo(FileWriteStrategy.directDuringTransfer);

		setProperty(createFile(child_1, "cloudstore.properties"), testKey2, "    55588  ");
		assertThat(config_1_2_aaa.getPropertyAsLong(testKey2, -1)).isEqualTo(55588);
	}

	private static void waitForDifferentLastModifiedTimestamp() {
		// Make sure the file has a different lastModified-timestamp! Most file systems have a timestamp-granularity of 1 second.
		try {
			Thread.sleep(1000);
		} catch (final InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	private File newTestRepositoryLocalRoot() throws IOException {
		return newTestRepositoryLocalRoot("");
	}

	private static void setGlobalProperty(final String key, final String value) throws IOException {
		setProperty(Config.getInstance().propertiesFiles[0], key, value);
	}

	private static void setProperty(final File propertiesFile, final String key, final String value) throws IOException {
		assertNotNull("propertiesFile", propertiesFile);
		assertNotNull("key", key);

		final Properties properties = new Properties();
		if (propertiesFile.exists()) {
			final InputStream in = propertiesFile.createFileInputStream();
			properties.load(in);
			in.close();
		}

		if (value == null)
			properties.remove(key);
		else
			properties.setProperty(key, value);

		final OutputStream out = propertiesFile.createFileOutputStream();
		properties.store(out, null);
		out.close();
	}

}
