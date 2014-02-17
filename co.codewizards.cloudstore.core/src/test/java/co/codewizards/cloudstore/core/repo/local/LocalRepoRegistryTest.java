package co.codewizards.cloudstore.core.repo.local;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.AbstractTest;

public class LocalRepoRegistryTest extends AbstractTest
{
	private static final Logger logger = LoggerFactory.getLogger(LocalRepoRegistryTest.class);

	@Test
	public void addNewLocalRepository() throws Exception {
		Map<UUID, File> newRepositoryId2FileMap = new HashMap<UUID, File>();

		File localRoot1 = newTestRepositoryLocalRoot();
		assertThat(localRoot1).doesNotExist();
		localRoot1.mkdirs();
		assertThat(localRoot1).isDirectory();
		LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForNewRepository(localRoot1);
		assertThat(localRepoManager).isNotNull();
		newRepositoryId2FileMap.put(localRepoManager.getRepositoryId(), localRoot1.getAbsoluteFile());

		File localRoot2 = newTestRepositoryLocalRoot();
		assertThat(localRoot2).doesNotExist();
		localRoot2.mkdirs();
		assertThat(localRoot2).isDirectory();
		LocalRepoManager localRepoManager2 = localRepoManagerFactory.createLocalRepoManagerForNewRepository(localRoot2);
		assertThat(localRepoManager).isNotNull();
		newRepositoryId2FileMap.put(localRepoManager2.getRepositoryId(), localRoot2.getAbsoluteFile());


		Set<Entry<UUID, File>> newEntrySet = newRepositoryId2FileMap.entrySet();
		for (Entry<UUID, File> newEntry : newEntrySet) {
			File localRoot = LocalRepoRegistry.getInstance().getLocalRootOrFail(newEntry.getKey());
			assertThat(localRoot).isEqualTo(newEntry.getValue());
		}
	}

	private File newTestRepositoryLocalRoot() throws IOException {
		return newTestRepositoryLocalRoot("");
	}

}
