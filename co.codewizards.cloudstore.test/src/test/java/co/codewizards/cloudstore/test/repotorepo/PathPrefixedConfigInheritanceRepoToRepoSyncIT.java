package co.codewizards.cloudstore.test.repotorepo;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static org.assertj.core.api.Assertions.*;

import java.util.Properties;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.config.Config;
import co.codewizards.cloudstore.core.config.ConfigImpl;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.progress.LoggerProgressMonitor;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.util.PropertiesUtil;

public class PathPrefixedConfigInheritanceRepoToRepoSyncIT extends AbstractRepoToRepoSyncIT
{
	private static final Logger logger = LoggerFactory.getLogger(PathPrefixedConfigInheritanceRepoToRepoSyncIT.class);

	@Test
	public void parentConfigWithRemotePathPrefix() throws Exception {
		remotePathPrefix = "/2/1 {11 11ä11#+} 1";
		syncFromRemoteToLocal();

		File dir2 = createFile(remoteRoot, "2");
		File dir2_1 = createFile(dir2, "1 {11 11ä11#+} 1");
		assertThat(dir2_1.getIoFile()).exists().isDirectory();

		File dir2_1_d1 = createDirectory(dir2_1, "d1");
		File dir2_1_d2 = createDirectory(dir2_1, "d2");
		File dir2_1_d3 = createDirectory(dir2_1, "d3");

		File dir2_1_d1_d10 = createDirectory(dir2_1_d1, "d10");
		File dir2_1_d1_d11 = createDirectory(dir2_1_d1, "d11");
		File dir2_1_d1_f20 = createFileWithRandomContent(dir2_1_d1, "f20");
		File dir2_1_d1_f21 = createFileWithRandomContent(dir2_1_d1, "f21");

		File dir2_1_d1_d10_f30 = createFileWithRandomContent(dir2_1_d1_d10, "f30");

		File dir2_1_d2_d20 = createDirectory(dir2_1_d2, "d20");
		File dir2_1_d2_f40 = createDirectory(dir2_1_d2, "f40");


		// Create and sync ignore rules.
		Properties properties = new Properties();
		properties.put("ignore[bla].namePattern", "bla");
		properties.put("ignore[test].namePattern", "test");
		properties.put("ignore[blubb].namePattern", "blubb");
		PropertiesUtil.store(createFile(remoteRoot, ".cloudstore.properties"), properties, null);

		properties = new Properties();
		properties.put("ignore[test].namePattern", "overwrittenTest");
		properties.put("ignore[blubb].namePattern", "overwrittenBlubb");
		properties.put("ignore[oink].namePattern", "oink");
		PropertiesUtil.store(createFile(dir2, ".cloudstore.properties"), properties, null);

		// The above 2 should be merged into the parent.properties, but the
		// following should not, because it's synced normally.
		properties = new Properties();
		properties.put("ignore[test].namePattern", "overwritten222");
		properties.put("ignore[bak].namePattern", "*.bak");
		PropertiesUtil.store(createFile(dir2_1, ".cloudstore.properties"), properties, null);

		// sync again
		repoToRepoSync.sync(new LoggerProgressMonitor(logger));

		File parentPropsFile = createFile(localRoot, LocalRepoManager.META_DIR_NAME, "parent.properties");
		assertThat(parentPropsFile.getIoFile()).exists().isFile();

		Properties parentProps = PropertiesUtil.load(parentPropsFile);
		assertThat(parentProps.getProperty("ignore[bla].namePattern", null)).isEqualTo("bla");
		assertThat(parentProps.getProperty("ignore[blubb].namePattern", null)).isEqualTo("overwrittenBlubb");
		assertThat(parentProps.getProperty("ignore[test].namePattern", null)).isEqualTo("overwrittenTest");
		assertThat(parentProps.getProperty("ignore[bak].namePattern", null)).isNull();

		Config configForRoot = ConfigImpl.getInstanceForDirectory(localRoot);
		assertThat(configForRoot.getProperty("ignore[bla].namePattern", null)).isEqualTo("bla");
		assertThat(configForRoot.getProperty("ignore[blubb].namePattern", null)).isEqualTo("overwrittenBlubb");
		assertThat(configForRoot.getProperty("ignore[test].namePattern", null)).isEqualTo("overwritten222");
		assertThat(configForRoot.getProperty("ignore[bak].namePattern", null)).isEqualTo("*.bak");
	}

	@Test
	public void parentConfigWithLocalPathPrefix() throws Exception {
		localPathPrefix = "/2/1 {11 11ä11#+} 1";
		syncFromLocalToRemote();

		File dir2 = createFile(localRoot, "2");
		File dir2_1 = createFile(dir2, "1 {11 11ä11#+} 1");
		assertThat(dir2_1.getIoFile()).exists().isDirectory();

		File dir2_1_d1 = createDirectory(dir2_1, "d1");
		File dir2_1_d2 = createDirectory(dir2_1, "d2");
		File dir2_1_d3 = createDirectory(dir2_1, "d3");

		File dir2_1_d1_d10 = createDirectory(dir2_1_d1, "d10");
		File dir2_1_d1_d11 = createDirectory(dir2_1_d1, "d11");
		File dir2_1_d1_f20 = createFileWithRandomContent(dir2_1_d1, "f20");
		File dir2_1_d1_f21 = createFileWithRandomContent(dir2_1_d1, "f21");

		File dir2_1_d1_d10_f30 = createFileWithRandomContent(dir2_1_d1_d10, "f30");

		File dir2_1_d2_d20 = createDirectory(dir2_1_d2, "d20");
		File dir2_1_d2_f40 = createDirectory(dir2_1_d2, "f40");


		// Create and sync ignore rules.
		Properties properties = new Properties();
		properties.put("ignore[bla].namePattern", "bla");
		properties.put("ignore[test].namePattern", "test");
		properties.put("ignore[blubb].namePattern", "blubb");
		PropertiesUtil.store(createFile(localRoot, ".cloudstore.properties"), properties, null);

		properties = new Properties();
		properties.put("ignore[test].namePattern", "overwrittenTest");
		properties.put("ignore[blubb].namePattern", "overwrittenBlubb");
		properties.put("ignore[oink].namePattern", "oink");
		PropertiesUtil.store(createFile(dir2, ".cloudstore.properties"), properties, null);

		// The above 2 should be merged into the parent.properties, but the
		// following should not, because it's synced normally.
		properties = new Properties();
		properties.put("ignore[test].namePattern", "overwritten222");
		properties.put("ignore[bak].namePattern", "*.bak");
		PropertiesUtil.store(createFile(dir2_1, ".cloudstore.properties"), properties, null);

		// sync again
		repoToRepoSync.sync(new LoggerProgressMonitor(logger));

		File parentPropsFile = createFile(remoteRoot, LocalRepoManager.META_DIR_NAME, "parent.properties");
		assertThat(parentPropsFile.getIoFile()).exists().isFile();

		Properties parentProps = PropertiesUtil.load(parentPropsFile);
		assertThat(parentProps.getProperty("ignore[bla].namePattern", null)).isEqualTo("bla");
		assertThat(parentProps.getProperty("ignore[blubb].namePattern", null)).isEqualTo("overwrittenBlubb");
		assertThat(parentProps.getProperty("ignore[test].namePattern", null)).isEqualTo("overwrittenTest");
		assertThat(parentProps.getProperty("ignore[bak].namePattern", null)).isNull();

		Config configForRoot = ConfigImpl.getInstanceForDirectory(remoteRoot);
		assertThat(configForRoot.getProperty("ignore[bla].namePattern", null)).isEqualTo("bla");
		assertThat(configForRoot.getProperty("ignore[blubb].namePattern", null)).isEqualTo("overwrittenBlubb");
		assertThat(configForRoot.getProperty("ignore[test].namePattern", null)).isEqualTo("overwritten222");
		assertThat(configForRoot.getProperty("ignore[bak].namePattern", null)).isEqualTo("*.bak");
	}
}
