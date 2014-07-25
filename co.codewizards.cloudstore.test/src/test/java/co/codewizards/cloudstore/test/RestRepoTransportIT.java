package co.codewizards.cloudstore.test;

import static org.assertj.core.api.Assertions.*;

import java.io.File;
import java.net.URL;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.transport.RepoTransport;
import co.codewizards.cloudstore.core.repo.transport.RepoTransportFactoryRegistry;
import co.codewizards.cloudstore.rest.client.ssl.CheckServerTrustedCertificateExceptionContext;
import co.codewizards.cloudstore.rest.client.ssl.CheckServerTrustedCertificateExceptionResult;
import co.codewizards.cloudstore.rest.client.ssl.DynamicX509TrustManagerCallback;
import co.codewizards.cloudstore.rest.client.transport.RestRepoTransport;
import co.codewizards.cloudstore.rest.client.transport.RestRepoTransportFactory;

public class RestRepoTransportIT extends AbstractIT {
	private static final Logger logger = LoggerFactory.getLogger(RestRepoTransportIT.class);

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
	public void getRepositoryId() throws Exception {
		final File remoteRoot = newTestRepositoryLocalRoot("");
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

}
