package co.codewizards.cloudstore.core.config;

import static co.codewizards.cloudstore.core.io.StreamUtil.*;
import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static java.util.Objects.*;
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

	private static final Object mutex = ConfigTest.class;

	/**
	 * Tests whether the global configuration file is named as documented on the
	 * web-site. If this test breaks, it must be verified, if the name really needs to be changed
	 * and if so, the documentation must be updated together with this test!!!
	 */
	@Test
	public void testGlobalConfigFileName() throws Exception {
		synchronized (mutex) {
			final ConfigImpl globalConfig = (ConfigImpl) ConfigImpl.getInstance();
			assertThat(globalConfig.propertiesFiles.length).isEqualTo(1);
			assertThat(globalConfig.propertiesFiles[0]).isNotNull();
			assertThat(globalConfig.propertiesFiles[0].getName()).isEqualTo("cloudstore.properties");

			deleteMainConfigFiles();
		}
	}

	@Test
	public void testGlobalConfigFileModification() throws Exception {
		synchronized (mutex) {
			final Config globalConfig = ConfigImpl.getInstance();
			final String testKey = "testKey0";
			String value = globalConfig.getProperty(testKey, null);
			assertThat(value).isNull();

			long version1 = globalConfig.getVersion();

			waitForDifferentLastModifiedTimestamp();

			setGlobalProperty(testKey, "testValueAAA");
			value = globalConfig.getProperty(testKey, null);
			assertThat(value).isEqualTo("testValueAAA");

			long version2 = globalConfig.getVersion();
			assertThat(version1).isNotEqualTo(version2);

			waitForDifferentLastModifiedTimestamp();

			setGlobalProperty(testKey, "testValueBBB");
			value = globalConfig.getProperty(testKey, null);
			assertThat(value).isEqualTo("testValueBBB");

			version1 = globalConfig.getVersion();
			assertThat(version1).isNotEqualTo(version2);

			deleteMainConfigFiles();
		}
	}

	@Test
	public void testConfigInheritance() throws Exception {
		synchronized (mutex) {
			final String testKey1 = "testKey1";
			final String testKey2 = "testKey2";

			final File localRoot = newTestRepositoryLocalRoot();
			assertThat(localRoot.exists()).isFalse();
			localRoot.mkdirs();
			assertThat(localRoot.isDirectory()).isTrue();

			try (final LocalRepoManager localRepoManager = LocalRepoManagerFactory.Helper.getInstance().createLocalRepoManagerForNewRepository(localRoot);) {
				assertThat(localRepoManager).isNotNull();

				final File child_1 = createFile(localRoot, "1_" + random.nextInt(10000));
				assertThat(child_1.exists()).isFalse();
				final Config config_1 = ConfigImpl.getInstanceForDirectory(child_1);
				assertThat(config_1.getPropertyAsNonEmptyTrimmedString(testKey1, null)).isNull();
				createDirectory(child_1);
				assertThat(child_1.isDirectory()).isTrue();
				setProperty(createFile(child_1, ".cloudstore.properties"), testKey1, "   testValueAAA     ");
				assertThat(config_1.getPropertyAsNonEmptyTrimmedString(testKey1, null)).isEqualTo("testValueAAA");
				setProperty(createFile(child_1, "cloudstore.properties"), testKey1, "    testValueBBB  ");
				assertThat(config_1.getPropertyAsNonEmptyTrimmedString(testKey1, null)).isEqualTo("testValueBBB");

				assertThat(config_1.getPropertyAsEnum(FileWriteStrategy.CONFIG_KEY, FileWriteStrategy.directAfterTransfer)).isEqualTo(FileWriteStrategy.directAfterTransfer);
				assertThat(config_1.getPropertyAsEnum(FileWriteStrategy.CONFIG_KEY, FileWriteStrategy.class, null)).isNull();

				long version1 = config_1.getVersion();

				waitForDifferentLastModifiedTimestamp();
				setGlobalProperty(FileWriteStrategy.CONFIG_KEY, FileWriteStrategy.replaceAfterTransfer.name());
				assertThat(config_1.getPropertyAsEnum(FileWriteStrategy.CONFIG_KEY, FileWriteStrategy.directAfterTransfer)).isEqualTo(FileWriteStrategy.replaceAfterTransfer);

				long version2 = config_1.getVersion();
				assertThat(version1).isNotEqualTo(version2);

				final File child_1_a = createFile(child_1, "a");
				final Config config_1_a = ConfigImpl.getInstanceForFile(child_1_a);

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

				final Config config_1_2_aaa = ConfigImpl.getInstanceForFile(child_1_2_aaa);
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

				deleteMainConfigFiles();
			}
		}
	}

	private void deleteMainConfigFiles() {
		for (final File file : ((ConfigImpl) ConfigImpl.getInstance()).propertiesFiles) {
			file.delete();
			assertThat(file.exists()).isFalse();
		}
	}

	private static void waitForDifferentLastModifiedTimestamp() {
		// Make sure the file has a different lastModified-timestamp! Most file systems have a timestamp-granularity of 1 second.
		try {
			Thread.sleep(1100);
		} catch (final InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	private File newTestRepositoryLocalRoot() throws IOException {
		return newTestRepositoryLocalRoot("");
	}

	private static void setGlobalProperty(final String key, final String value) throws IOException {
		setProperty(((ConfigImpl) ConfigImpl.getInstance()).propertiesFiles[0], key, value);
	}

	private static void setProperty(final File propertiesFile, final String key, final String value) throws IOException {
		requireNonNull(propertiesFile, "propertiesFile");
		requireNonNull(key, "key");

		final Properties properties = new Properties();
		if (propertiesFile.exists()) {
			final InputStream in = castStream(propertiesFile.createInputStream());
			properties.load(in);
			in.close();
		}

		if (value == null)
			properties.remove(key);
		else
			properties.setProperty(key, value);

		final OutputStream out = castStream(propertiesFile.createOutputStream());
		properties.store(out, null);
		out.close();
	}

}
