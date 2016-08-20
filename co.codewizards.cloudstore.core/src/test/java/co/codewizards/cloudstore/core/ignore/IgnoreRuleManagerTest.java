package co.codewizards.cloudstore.core.ignore;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import org.junit.After;
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
	private Properties configProps;
	private File tempDir;
	private static final AtomicLong configVersion = new AtomicLong();

	@Before
	public void before() throws Exception {
		System.out.println();
		System.out.println(">>> before >>>");

		tempDir = createTempDirectory("oink");
		File f = createFile(tempDir, "bla.properties");
		configVersion.incrementAndGet();
		final Config config = new ConfigImpl(null, tempDir, new File[] { f }) {
			{
				configProps = properties;
			}

			@Override
			public Map<String, List<String>> getKey2GroupsMatching(Pattern regex) {
				return super.getKey2GroupsMatching(regex);
			}

			@Override
			public long getVersion() {
				return configVersion.get();
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
			void readIfNeeded() { }

			@Mock
			void read() { }

			@Mock
			void write() { }

			@Mock
			Config getInstance(final File file, final boolean isDirectory) {
				return config;
			}
		};
	}

	@After
	public void after() throws Exception {
		if (tempDir != null)
			tempDir.deleteRecursively();

		tempDir = null;
		System.out.println("<<< after <<<");
		System.out.println();
	}

	@Test
	public void shellPattern1() throws Exception {
		System.out.println("*** shellPattern1 ***");
		configProps.put("ignore[0].namePattern", "*.jpg");
		configProps.put("ignore[1].namePattern", "*.bmp");
		configProps.put("ignore[1].enabled", "false");
		configProps.put("ignore[2].namePattern", "*.png");
		configProps.put("ignore[2].caseSensitive", "true");

		File directory = createFile(tempDir, "dummy");
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
		System.out.println("*** shellPattern2 ***");
		configProps.put("ignore[0].namePattern", "[A-Z].jpg");

		File directory = createFile(tempDir, "dummy");
		IgnoreRuleManager ignoreRuleManager = IgnoreRuleManagerImpl.getInstanceForDirectory(directory);
		assertThat(ignoreRuleManager.isIgnored(createFile(directory, "a.jpg"))).isTrue();
		assertThat(ignoreRuleManager.isIgnored(createFile(directory, "ab.jpg"))).isFalse();
	}

	@Test
	public void shellPattern3() throws Exception {
		System.out.println("*** shellPattern3 ***");
		configProps.put("ignore[0].namePattern", "[a-z]*.jpg");

		File directory = createFile(tempDir, "dummy");
		IgnoreRuleManager ignoreRuleManager = IgnoreRuleManagerImpl.getInstanceForDirectory(directory);
		assertThat(ignoreRuleManager.isIgnored(createFile(directory, "a.jpg"))).isTrue();
		assertThat(ignoreRuleManager.isIgnored(createFile(directory, "ab.jpg"))).isTrue();
		assertThat(ignoreRuleManager.isIgnored(createFile(directory, "a7.jpg"))).isTrue();
	}

	@Test
	public void shellPattern4() throws Exception {
		System.out.println("*** shellPattern4 ***");
		configProps.put("ignore[0].namePattern", "[acg].jpg");

		File directory = createFile(tempDir, "dummy");
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
		System.out.println("*** regex1 ***");
		configProps.put("ignore[0].nameRegex", ".*\\.jpg");
		configProps.put("ignore[1].nameRegex", ".*\\.bmp");
		configProps.put("ignore[1].enabled", "false");
		configProps.put("ignore[2].nameRegex", ".*\\.png");
		configProps.put("ignore[2].caseSensitive", "true");

		File directory = createFile(tempDir, "dummy");
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
		System.out.println("*** regex2 ***");
		configProps.put("ignore[0].nameRegex", "[a-z]{2}[0-9]{1}\\.jpg");

		File directory = createFile(tempDir, "dummy");
		IgnoreRuleManager ignoreRuleManager = IgnoreRuleManagerImpl.getInstanceForDirectory(directory);
		assertThat(ignoreRuleManager.isIgnored(createFile(directory, "ab1.jpg"))).isTrue();
		assertThat(ignoreRuleManager.isIgnored(createFile(directory, "abc1jpg"))).isFalse();
		assertThat(ignoreRuleManager.isIgnored(createFile(directory, "ab11.jpg"))).isFalse();
		assertThat(ignoreRuleManager.isIgnored(createFile(directory, "a1.jpg"))).isFalse();
	}
}
