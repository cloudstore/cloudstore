package co.codewizards.cloudstore.core.ignore;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static org.assertj.core.api.Assertions.*;

import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import co.codewizards.cloudstore.core.config.Config;
import co.codewizards.cloudstore.core.config.ConfigImpl;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.local.LocalRepoHelper;
import mockit.Mock;
import mockit.MockUp;
import mockit.integration.junit4.JMockit;

@RunWith(JMockit.class)
public class IgnoreRuleManagerTest {
	private Properties configProps = new Properties();
	private File tempDir;

	@Before
	public void before() throws Exception {
		tempDir = createTempDirectory("oink");
		File f = createFile(tempDir, "bla.properties");
		final Config config = new ConfigImpl(null, tempDir, new File[] { f }) {
			long version;
			@Override
			public synchronized long getVersion() {
				return ++version;
			}
		};

		new MockUp<LocalRepoHelper>() {
			@Mock
			File getLocalRootContainingFile(final File file) {
				return file;
			}
		};

		new MockUp<ConfigImpl>() {
			@Mock
			String getProperty(final String key, final String defaultValue) {
				return configProps.getProperty(key, defaultValue);
			}

			@Mock
			String getPropertyAsNonEmptyTrimmedString(final String key, final String defaultValue) {
				return configProps.getProperty(key, defaultValue);
			}

			@Mock
			Config getInstance(final File file, final boolean isDirectory) {
				return config;
			}
		};
	}

	public void after() throws Exception {
		if (tempDir != null)
			tempDir.deleteRecursively();

		tempDir = null;
	}

	@Test
	public void shellPattern1() throws Exception {
		configProps.put("ignore[0].namePattern", "*.jpg");
		configProps.put("ignore[1].namePattern", "*.bmp");
		configProps.put("ignore[1].enabled", "false");
		configProps.put("ignore[2].namePattern", "*.png");
		configProps.put("ignore[2].caseSensitive", "true");

		File directory = createFile("dummy");
		IgnoreRuleManager ignoreRuleManager = IgnoreRuleManagerImpl.getInstanceForDirectory(directory);
		assertThat(ignoreRuleManager.isIgnored(createFile(directory, "abc.jpg"))).isTrue();
		assertThat(ignoreRuleManager.isIgnored(createFile(directory, "abcxjpg"))).isFalse();
		assertThat(ignoreRuleManager.isIgnored(createFile(directory, "ABC.JPG"))).isTrue();
		assertThat(ignoreRuleManager.isIgnored(createFile(directory, "abc.bmp"))).isFalse();
		assertThat(ignoreRuleManager.isIgnored(createFile(directory, "abc.png"))).isTrue();
		assertThat(ignoreRuleManager.isIgnored(createFile(directory, "ABC.PNG"))).isFalse();
	}

	@Test
	public void shellPattern2() throws Exception {
		configProps.put("ignore[0].namePattern", "[A-Z].jpg");

		File directory = createFile("dummy");
		IgnoreRuleManager ignoreRuleManager = IgnoreRuleManagerImpl.getInstanceForDirectory(directory);
		assertThat(ignoreRuleManager.isIgnored(createFile(directory, "a.jpg"))).isTrue();
		assertThat(ignoreRuleManager.isIgnored(createFile(directory, "ab.jpg"))).isFalse();
	}

	@Test
	public void shellPattern3() throws Exception {
		configProps.put("ignore[0].namePattern", "[a-z]*.jpg");

		File directory = createFile("dummy");
		IgnoreRuleManager ignoreRuleManager = IgnoreRuleManagerImpl.getInstanceForDirectory(directory);
		assertThat(ignoreRuleManager.isIgnored(createFile(directory, "a.jpg"))).isTrue();
		assertThat(ignoreRuleManager.isIgnored(createFile(directory, "ab.jpg"))).isTrue();
		assertThat(ignoreRuleManager.isIgnored(createFile(directory, "a7.jpg"))).isTrue();
	}

	@Test
	public void shellPattern4() throws Exception {
		configProps.put("ignore[0].namePattern", "[acg].jpg");

		File directory = createFile("dummy");
		IgnoreRuleManager ignoreRuleManager = IgnoreRuleManagerImpl.getInstanceForDirectory(directory);
		assertThat(ignoreRuleManager.isIgnored(createFile(directory, "a.jpg"))).isTrue();
		assertThat(ignoreRuleManager.isIgnored(createFile(directory, "ab.jpg"))).isFalse();
		assertThat(ignoreRuleManager.isIgnored(createFile(directory, "b.jpg"))).isFalse();
		assertThat(ignoreRuleManager.isIgnored(createFile(directory, "c.jpg"))).isTrue();
		assertThat(ignoreRuleManager.isIgnored(createFile(directory, "d.jpg"))).isFalse();
		assertThat(ignoreRuleManager.isIgnored(createFile(directory, "e.jpg"))).isFalse();
		assertThat(ignoreRuleManager.isIgnored(createFile(directory, "f.jpg"))).isFalse();
		assertThat(ignoreRuleManager.isIgnored(createFile(directory, "g.jpg"))).isTrue();
	}

	@Test
	public void regex1() throws Exception {
		configProps.put("ignore[0].nameRegex", ".*\\.jpg");
		configProps.put("ignore[1].nameRegex", ".*\\.bmp");
		configProps.put("ignore[1].enabled", "false");
		configProps.put("ignore[2].nameRegex", ".*\\.png");
		configProps.put("ignore[2].caseSensitive", "true");

		File directory = createFile("dummy");
		IgnoreRuleManager ignoreRuleManager = IgnoreRuleManagerImpl.getInstanceForDirectory(directory);
		assertThat(ignoreRuleManager.isIgnored(createFile(directory, "abc.jpg"))).isTrue();
		assertThat(ignoreRuleManager.isIgnored(createFile(directory, "abcxjpg"))).isFalse();
		assertThat(ignoreRuleManager.isIgnored(createFile(directory, "ABC.JPG"))).isTrue();
		assertThat(ignoreRuleManager.isIgnored(createFile(directory, "abc.bmp"))).isFalse();
		assertThat(ignoreRuleManager.isIgnored(createFile(directory, "abc.png"))).isTrue();
		assertThat(ignoreRuleManager.isIgnored(createFile(directory, "ABC.PNG"))).isFalse();
	}

	@Test
	public void regex2() throws Exception {
		configProps.put("ignore[0].nameRegex", "[a-z]{2}[0-9]{1}\\.jpg");

		File directory = createFile("dummy");
		IgnoreRuleManager ignoreRuleManager = IgnoreRuleManagerImpl.getInstanceForDirectory(directory);
		assertThat(ignoreRuleManager.isIgnored(createFile(directory, "ab1.jpg"))).isTrue();
		assertThat(ignoreRuleManager.isIgnored(createFile(directory, "abc1jpg"))).isFalse();
		assertThat(ignoreRuleManager.isIgnored(createFile(directory, "ab11.jpg"))).isFalse();
		assertThat(ignoreRuleManager.isIgnored(createFile(directory, "a1.jpg"))).isFalse();
	}
}
