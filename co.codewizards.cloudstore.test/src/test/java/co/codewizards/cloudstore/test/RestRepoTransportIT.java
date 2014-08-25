package co.codewizards.cloudstore.test;

import static org.assertj.core.api.Assertions.*;

import java.net.URL;
import java.util.UUID;

import org.junit.Test;

import co.codewizards.cloudstore.client.CloudStoreClient;
import co.codewizards.cloudstore.core.dto.RepoFileDto;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.transport.RepoTransport;
import co.codewizards.cloudstore.core.repo.transport.RepoTransportFactoryRegistry;
import co.codewizards.cloudstore.local.transport.FileRepoTransport;
import co.codewizards.cloudstore.oio.api.File;
import co.codewizards.cloudstore.rest.client.ssl.CheckServerTrustedCertificateExceptionContext;
import co.codewizards.cloudstore.rest.client.ssl.CheckServerTrustedCertificateExceptionResult;
import co.codewizards.cloudstore.rest.client.ssl.DynamicX509TrustManagerCallback;
import co.codewizards.cloudstore.rest.client.transport.RestRepoTransport;

public class RestRepoTransportIT extends AbstractIT {

	public static class TestDynamicX509TrustManagerCallback implements DynamicX509TrustManagerCallback {
		@Override
		public CheckServerTrustedCertificateExceptionResult handleCheckServerTrustedCertificateException(final CheckServerTrustedCertificateExceptionContext context) {
			final CheckServerTrustedCertificateExceptionResult result = new CheckServerTrustedCertificateExceptionResult();
			result.setTrusted(true);
			return result;
		}
	}

	@Test
	public void getRepositoryId_Rest() throws Exception {
		final File remoteRoot = newTestRepositoryLocalRoot("remote");
		assertThat(remoteRoot.exists()).isFalse();
		remoteRoot.mkdirs();
		assertThat(remoteRoot.isDirectory()).isTrue();

		final LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForNewRepository(remoteRoot);
		assertThat(localRepoManager).isNotNull();
		final UUID remoteRepositoryId = localRepoManager.getRepositoryId();
		localRepoManager.close();

		final URL remoteRootURL = new URL(getSecureUrl() + "/" + remoteRepositoryId);

		final RepoTransport repoTransport = RepoTransportFactoryRegistry.getInstance().getRepoTransportFactory(remoteRootURL).createRepoTransport(remoteRootURL, null);
		assertThat(repoTransport).isInstanceOf(RestRepoTransport.class);
		final UUID repositoryId = repoTransport.getRepositoryId();
		assertThat(repositoryId).isEqualTo(remoteRepositoryId);

		repoTransport.close();
	}

	@Test
	public void getRepoFileDtoForNonExistingFile() throws Exception {
		final File localRoot = newTestRepositoryLocalRoot("local");
		assertThat(localRoot.exists()).isFalse();
		localRoot.mkdirs();

		final LocalRepoManager localRepoManagerLocal = localRepoManagerFactory.createLocalRepoManagerForNewRepository(localRoot);
		final UUID localRepositoryId = localRepoManagerLocal.getRepositoryId();
		localRepoManagerLocal.close();

		final File remoteRoot = newTestRepositoryLocalRoot("remote");
		assertThat(remoteRoot.exists()).isFalse();
		remoteRoot.mkdirs();
		assertThat(remoteRoot.isDirectory()).isTrue();

		final LocalRepoManager localRepoManagerRemote = localRepoManagerFactory.createLocalRepoManagerForNewRepository(remoteRoot);
		assertThat(localRepoManagerRemote).isNotNull();
		final UUID remoteRepositoryId = localRepoManagerRemote.getRepositoryId();
		localRepoManagerRemote.close();

		final URL remoteRootURL = new URL(getSecureUrl() + "/" + remoteRepositoryId);

		new CloudStoreClient("requestRepoConnection", localRoot.getPath(), remoteRootURL.toExternalForm()).execute();
		new CloudStoreClient("acceptRepoConnection", remoteRoot.getPath()).execute();

		final RepoTransport repoTransport = RepoTransportFactoryRegistry.getInstance().getRepoTransportFactory(remoteRootURL).createRepoTransport(remoteRootURL, localRepositoryId);
		assertThat(repoTransport).isInstanceOf(RestRepoTransport.class);

		final RepoFileDto repoFileDto = repoTransport.getRepoFileDto("/this/does/not/exist");
		assertThat(repoFileDto).isNull();

		repoTransport.close();
	}

	@Test
	public void getRepositoryId_File() throws Exception {
		final File localRoot = newTestRepositoryLocalRoot("local");
		assertThat(localRoot.exists()).isFalse();
		localRoot.mkdirs();
		assertThat(localRoot.isDirectory()).isTrue();
		final URL localRootURL = localRoot.toURI().toURL();

		final LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForNewRepository(localRoot);
		assertThat(localRepoManager).isNotNull();
		final UUID repositoryId = localRepoManager.getRepositoryId();
		localRepoManager.close();

		final RepoTransport repoTransport = RepoTransportFactoryRegistry.getInstance().getRepoTransportFactory(localRootURL).createRepoTransport(localRootURL, null);
		assertThat(repoTransport).isInstanceOf(FileRepoTransport.class);
		final UUID repositoryIdFromTransport = repoTransport.getRepositoryId();
		assertThat(repositoryIdFromTransport).isEqualTo(repositoryId);

		repoTransport.close();
	}

}
