package co.codewizards.cloudstore.local;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.config.Config;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoRegistry;
import co.codewizards.cloudstore.oio.api.File;

public class LocalRepoRegistryTest extends AbstractTest
{
	private static final Logger logger = LoggerFactory.getLogger(LocalRepoRegistryTest.class);

	@BeforeClass
	public static void beforeClass() {
		System.setProperty(LocalRepoManager.SYSTEM_PROPERTY_CLOSE_DEFERRED_MILLIS, "0");
		System.setProperty(
				Config.SYSTEM_PROPERTY_PREFIX + LocalRepoRegistry.CONFIG_KEY_EVICT_DEAD_ENTRIES_PERIOD, "0");
	}
	@AfterClass
	public static void afterClass() {
		System.getProperties().remove(LocalRepoManager.SYSTEM_PROPERTY_CLOSE_DEFERRED_MILLIS);
		System.getProperties().remove(
				Config.SYSTEM_PROPERTY_PREFIX + LocalRepoRegistry.CONFIG_KEY_EVICT_DEAD_ENTRIES_PERIOD);

	}

	@Test
	public void createLocalRepositories() throws Exception {
		final Map<UUID, File> newRepositoryId2FileMap = new HashMap<UUID, File>();

		final File localRoot1 = newTestRepositoryLocalRoot();
		assertThat(localRoot1.exists()).isFalse();
		localRoot1.mkdirs();
		assertThat(localRoot1.isDirectory()).isTrue();
		final LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForNewRepository(localRoot1);
		assertThat(localRepoManager).isNotNull();
		newRepositoryId2FileMap.put(localRepoManager.getRepositoryId(), localRoot1.getAbsoluteFile());
		localRepoManager.close();

		final File localRoot2 = newTestRepositoryLocalRoot();
		assertThat(localRoot2.exists()).isFalse();
		localRoot2.mkdirs();
		assertThat(localRoot2.isDirectory()).isTrue();
		final LocalRepoManager localRepoManager2 = localRepoManagerFactory.createLocalRepoManagerForNewRepository(localRoot2);
		assertThat(localRepoManager2).isNotNull();
		newRepositoryId2FileMap.put(localRepoManager2.getRepositoryId(), localRoot2.getAbsoluteFile());
		localRepoManager2.close();

		final Set<Entry<UUID, File>> newEntrySet = newRepositoryId2FileMap.entrySet();
		for (final Entry<UUID, File> newEntry : newEntrySet) {
			final File localRoot = LocalRepoRegistry.getInstance().getLocalRootOrFail(newEntry.getKey());
			assertThat(localRoot).isEqualTo(newEntry.getValue());
		}
	}

	@Test
	public void moveLocalRepositoryWithAliases() throws Exception {
		final LocalRepoRegistry localRepoRegistry = LocalRepoRegistry.getInstance();
		final File localRootOld = newTestRepositoryLocalRoot().getCanonicalFile();
		assertThat(localRootOld.exists()).isFalse();
		localRootOld.mkdirs();
		assertThat(localRootOld.isDirectory()).isTrue();
		LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForNewRepository(localRootOld);
		final UUID repositoryId = localRepoManager.getRepositoryId();
		assertThat(localRepoManager).isNotNull();
		final String aliasPrefix = Long.toString(System.currentTimeMillis(), 36);
		final String alias1 = aliasPrefix + "_alias1";
		final String alias2 = aliasPrefix + "_alias2";
		localRepoManager.putRepositoryAlias(alias1);
		localRepoManager.putRepositoryAlias(alias2);
		localRepoManager.close();

		assertThat(localRepoRegistry.getRepositoryId(alias1)).isEqualTo(repositoryId);
		assertThat(localRepoRegistry.getRepositoryId(alias2)).isEqualTo(repositoryId);
		assertThat(localRepoRegistry.getLocalRootForRepositoryName(alias1)).isEqualTo(localRootOld);
		assertThat(localRepoRegistry.getLocalRootForRepositoryName(alias2)).isEqualTo(localRootOld);

		final File localRootNew = newTestRepositoryLocalRoot().getCanonicalFile();
		assertThat(localRootNew.exists()).isFalse();
		localRootOld.renameTo(localRootNew);
		assertThat(localRootOld.exists()).isFalse();
		assertThat(localRootNew.isDirectory()).isTrue();

		final File localRoot2 = newTestRepositoryLocalRoot().getCanonicalFile();
		localRoot2.mkdir();
		final LocalRepoManager localRepoManager2 = localRepoManagerFactory.createLocalRepoManagerForNewRepository(localRoot2);
		localRepoManager2.putRepositoryAlias(Long.toString(System.currentTimeMillis(), 36));
		localRepoManager2.close();

		localRepoManager = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(localRootNew);
		localRepoManager.close();

		assertThat(localRepoRegistry.getRepositoryId(alias1)).isEqualTo(repositoryId);
		assertThat(localRepoRegistry.getRepositoryId(alias2)).isEqualTo(repositoryId);
		assertThat(localRepoRegistry.getLocalRootForRepositoryName(alias1)).isEqualTo(localRootNew);
		assertThat(localRepoRegistry.getLocalRootForRepositoryName(alias2)).isEqualTo(localRootNew);
	}

	private File newTestRepositoryLocalRoot() throws IOException {
		return newTestRepositoryLocalRoot("");
	}
}
