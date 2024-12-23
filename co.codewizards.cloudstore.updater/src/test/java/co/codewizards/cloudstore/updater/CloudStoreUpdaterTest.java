package co.codewizards.cloudstore.updater;

import static java.util.Objects.*;
import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.util.Properties;

import org.junit.Test;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.updater.CloudStoreUpdaterCore;
import co.codewizards.cloudstore.core.util.PropertiesUtil;
import co.codewizards.cloudstore.core.version.Version;
import mockit.Invocation;
import mockit.Mock;
import mockit.MockUp;

//@RunWith(JMockit.class)
public class CloudStoreUpdaterTest extends AbstractTestWithTempDir {

	/**
	 * The new version to which we update. This overrides the information that would otherwise be downloaded
	 * from our server (e.g. from
	 * <a href="http://cloudstore.codewizards.co/update/co.codewizards.cloudstore.server/version">http://cloudstore.codewizards.co/update/co.codewizards.cloudstore.server/version</a>).
	 */
	private Version remoteVersion;
	private File installationDir;

	@Override
	public void before() throws Exception {
		super.before();

		new MockUp<CloudStoreUpdaterCore>() {
			@Mock
			Version getRemoteVersion(Invocation invocation) {
				return requireNonNull(remoteVersion, "remoteVersion");
			}
		};
	}

	@Test
	public void update_server_from_0_9_6_to_0_9_7() throws Exception {
		remoteVersion = new Version("0.9.7");
		File oldTarGzFile = downloadFileToTempDir("http://cloudstore.codewizards.co/0.9.6/download/co.codewizards.cloudstore.server-0.9.6-bin.tar.gz");

		install(oldTarGzFile);

		assertThatInstallationIs("co.codewizards.cloudstore.server", "0.9.6");

		new CloudStoreUpdater(new String[] { "-installationDir", installationDir.getAbsolutePath() }).execute();

		assertThatInstallationIs("co.codewizards.cloudstore.server", "0.9.7");
	}

	@Test
	public void update_client_from_0_9_6_to_0_9_7() throws Exception {
		remoteVersion = new Version("0.9.7");
		File oldTarGzFile = downloadFileToTempDir("http://cloudstore.codewizards.co/0.9.6/download/co.codewizards.cloudstore.client-0.9.6-bin.tar.gz");

		install(oldTarGzFile);

		assertThatInstallationIs("co.codewizards.cloudstore.client", "0.9.6");

		new CloudStoreUpdater(new String[] { "-installationDir", installationDir.getAbsolutePath() }).execute();

		assertThatInstallationIs("co.codewizards.cloudstore.client", "0.9.7");
	}

	@Test
	public void update_server_from_0_9_13_to_0_10_0() throws Exception {
		remoteVersion = new Version("0.10.0");
		File oldTarGzFile = downloadFileToTempDir("http://cloudstore.codewizards.co/0.9.13/download/co.codewizards.cloudstore.server-0.9.13-bin.tar.gz");

		install(oldTarGzFile);

		assertThatInstallationIs("co.codewizards.cloudstore.server", "0.9.13");

		new CloudStoreUpdater(new String[] { "-installationDir", installationDir.getAbsolutePath() }).execute();

		assertThatInstallationIs("co.codewizards.cloudstore.server", "0.10.0");
	}

	@Test
	public void update_client_from_0_9_13_to_0_10_0() throws Exception {
		remoteVersion = new Version("0.10.0");
		File oldTarGzFile = downloadFileToTempDir("http://cloudstore.codewizards.co/0.9.13/download/co.codewizards.cloudstore.client-0.9.13-bin.tar.gz");

		install(oldTarGzFile);

		assertThatInstallationIs("co.codewizards.cloudstore.client", "0.9.13");

		new CloudStoreUpdater(new String[] { "-installationDir", installationDir.getAbsolutePath() }).execute();

		assertThatInstallationIs("co.codewizards.cloudstore.client", "0.10.0");
	}

	private void install(File oldTarGzFile) throws IOException {
		File installationBaseDir = tempDir.createFile("installation");
		new TarGzFile(oldTarGzFile).extract(installationBaseDir);
		File cloudstoreSubDir = installationBaseDir.createFile("cloudstore");
		assertThat(cloudstoreSubDir.getIoFile()).isDirectory();
		this.installationDir = cloudstoreSubDir;
	}

	private void assertThatInstallationIs(String expectedArtifactId, String expectedVersion) throws IOException {
		requireNonNull(installationDir, "installationDir");

		File installationPropertiesFile = installationDir.createFile("installation.properties");
		assertThat(installationPropertiesFile.getIoFile()).exists();
		Properties installationProperties = PropertiesUtil.load(installationPropertiesFile);
		String installedArtifactId = installationProperties.getProperty("artifactId");
		String installedVersion = installationProperties.getProperty("version");
		assertThat(installedArtifactId).isEqualTo(expectedArtifactId);
		assertThat(installedVersion).isEqualTo(expectedVersion);

		File libDir = installationDir.createFile("lib");
		int cloudstoreLibCount = 0;
		int allLibCount = 0;
		String expectedLibFileSuffix = "-" + expectedVersion + ".jar";
		for (File libFile : libDir.listFiles()) {
			++allLibCount;
			if (libFile.getName().startsWith("co.codewizards.cloudstore.")) {
				++cloudstoreLibCount;
				assertThat(libFile.getName()).endsWith(expectedLibFileSuffix);
			}
		}
		assertThat(cloudstoreLibCount).isGreaterThanOrEqualTo(6);
		assertThat(allLibCount).isGreaterThanOrEqualTo(20);
	}
}
