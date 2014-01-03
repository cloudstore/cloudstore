package co.codewizards.cloudstore.shared.repo.transport;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import co.codewizards.cloudstore.shared.repo.transport.file.FileRepoTransportFactory;
import co.codewizards.cloudstore.shared.util.IOUtil;

public class RepoTransportFactoryRegistryTest {

	@Test
	public void ensureFileRepoTransportFactoryIsRegistered() {
		List<RepoTransportFactory> repoTransportFactories = RepoTransportFactoryRegistry.getInstance().getRepoTransportFactories();
		for (RepoTransportFactory repoTransportFactory : repoTransportFactories) {
			if (repoTransportFactory instanceof FileRepoTransportFactory) {
				return;
			}
		}
		Assert.fail("The FileRepoTransportFactory was not found!");
		assertThat(repoTransportFactories).hasSize(1);
	}

	@Test
	public void ensureFileRepoTransportFactoryIsReturnedForFileURL() throws Exception {
		List<RepoTransportFactory> repoTransportFactories = RepoTransportFactoryRegistry.getInstance().getRepoTransportFactories(IOUtil.getTempDir().toURI().toURL());
		assertThat(repoTransportFactories).isNotEmpty();
		assertThat(repoTransportFactories).hasSize(1);
		assertThat(repoTransportFactories.get(0)).isInstanceOf(FileRepoTransportFactory.class);

		RepoTransportFactory repoTransportFactory = RepoTransportFactoryRegistry.getInstance().getRepoTransportFactory(IOUtil.getTempDir().toURI().toURL());
		assertThat(repoTransportFactory).isInstanceOf(FileRepoTransportFactory.class);
	}

}
