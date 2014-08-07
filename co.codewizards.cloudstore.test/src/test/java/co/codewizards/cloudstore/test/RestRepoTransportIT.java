package co.codewizards.cloudstore.test;

import static org.assertj.core.api.Assertions.*;

import java.io.File;
import java.net.URL;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.transport.RepoTransport;
import co.codewizards.cloudstore.core.repo.transport.RepoTransportFactoryRegistry;
import co.codewizards.cloudstore.local.transport.FileRepoTransport;
import co.codewizards.cloudstore.rest.client.ssl.CheckServerTrustedCertificateExceptionContext;
import co.codewizards.cloudstore.rest.client.ssl.CheckServerTrustedCertificateExceptionResult;
import co.codewizards.cloudstore.rest.client.ssl.DynamicX509TrustManagerCallback;
import co.codewizards.cloudstore.rest.client.transport.RestRepoTransport;
import co.codewizards.cloudstore.rest.client.transport.RestRepoTransportFactory;

public class RestRepoTransportIT extends AbstractIT {

	public static class TestDynamicX509TrustManagerCallback implements DynamicX509TrustManagerCallback {
		@Override
		public CheckServerTrustedCertificateExceptionResult handleCheckServerTrustedCertificateException(final CheckServerTrustedCertificateExceptionContext context) {
			final CheckServerTrustedCertificateExceptionResult result = new CheckServerTrustedCertificateExceptionResult();
			result.setTrusted(true);
			return result;
		}
	}

	private static RestRepoTransportFactory restRepoTransportFactory;

	@BeforeClass
	public static void beforeClass() {
		restRepoTransportFactory = RepoTransportFactoryRegistry.getInstance().getRepoTransportFactoryOrFail(RestRepoTransportFactory.class);
		restRepoTransportFactory.setDynamicX509TrustManagerCallbackClass(TestDynamicX509TrustManagerCallback.class);
	}

	@AfterClass
	public static void afterClass() {
		restRepoTransportFactory.setDynamicX509TrustManagerCallbackClass(null);
	}

	@Test
	public void getRepositoryId_Rest() throws Exception {
		final File remoteRoot = newTestRepositoryLocalRoot("remote");
		assertThat(remoteRoot).doesNotExist();
		remoteRoot.mkdirs();
		assertThat(remoteRoot).isDirectory();

		final LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForNewRepository(remoteRoot);
		assertThat(localRepoManager).isNotNull();
		final UUID remoteRepositoryId = localRepoManager.getRepositoryId();

		final URL remoteRootURL = new URL(getSecureUrl() + "/" + remoteRepositoryId);

		final RepoTransport repoTransport = RepoTransportFactoryRegistry.getInstance().getRepoTransportFactory(remoteRootURL).createRepoTransport(remoteRootURL, null);
		assertThat(repoTransport).isInstanceOf(RestRepoTransport.class);
		final UUID repositoryId = repoTransport.getRepositoryId();
		assertThat(repositoryId).isEqualTo(remoteRepositoryId);

		repoTransport.close();
	}

	@Test
	public void getRepositoryId_File() throws Exception {
		File localRoot = newTestRepositoryLocalRoot("local");
		assertThat(localRoot).doesNotExist();
		localRoot.mkdirs();
		assertThat(localRoot).isDirectory();
		final URL localRootURL = localRoot.toURI().toURL();

		final LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForNewRepository(localRoot);
		assertThat(localRepoManager).isNotNull();
		final UUID repositoryId = localRepoManager.getRepositoryId();

		final RepoTransport repoTransport = RepoTransportFactoryRegistry.getInstance().getRepoTransportFactory(localRootURL).createRepoTransport(localRootURL, null);
		assertThat(repoTransport).isInstanceOf(FileRepoTransport.class);
		final UUID repositoryIdFromTransport = repoTransport.getRepositoryId();
		assertThat(repositoryIdFromTransport).isEqualTo(repositoryId);

		repoTransport.close();
	}

}
