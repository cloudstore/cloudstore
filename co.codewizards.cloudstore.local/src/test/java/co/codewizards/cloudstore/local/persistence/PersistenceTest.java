package co.codewizards.cloudstore.local.persistence;

import static co.codewizards.cloudstore.core.objectfactory.ObjectFactoryUtil.createObject;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.local.AbstractTest;

public class PersistenceTest extends AbstractTest {

	private static final int modificationCount = 10000;

	@BeforeClass
	public static void beforeClass() {
		System.setProperty(LocalRepoManager.SYSTEM_PROPERTY_CLOSE_DEFERRED_MILLIS, "0");
	}

	@AfterClass
	public static void afterClass() {
		System.clearProperty(LocalRepoManager.SYSTEM_PROPERTY_CLOSE_DEFERRED_MILLIS);
	}

	@Test
	public void getModifications() throws Exception {
		final File remoteRoot = newTestRepositoryLocalRoot("remote");
		remoteRoot.mkdirs();
		LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForNewRepository(remoteRoot);
		assertThat(localRepoManager).isNotNull();
		LocalRepoTransaction transaction = localRepoManager.beginWriteTransaction();
		try {
			RemoteRepository remoteRepository = createObject(RemoteRepository.class);
			remoteRepository.setLocalPathPrefix("");
			remoteRepository.setPublicKey(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 0 });
			remoteRepository = transaction.getDao(RemoteRepositoryDao.class).makePersistent(remoteRepository);

			final CopyModificationDao copyModificationDao = transaction.getDao(CopyModificationDao.class);
			for (int i = 0; i < modificationCount; ++i) {
				final CopyModification copyModification = new CopyModification();
				copyModification.setRemoteRepository(remoteRepository);
				copyModification.setFromPath("/from/" + i);
				copyModification.setToPath("/to/" + i);
				copyModification.setLength(100000);
				copyModification.setSha1("TEST" + i);
				copyModificationDao.makePersistent(copyModification);
			}
			transaction.commit();
		} finally {
			transaction.rollbackIfActive();
		}

		localRepoManager.close();
		localRepoManager = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(remoteRoot);

		transaction = localRepoManager.beginReadTransaction();
		try {
			final RemoteRepository remoteRepository = transaction.getDao(RemoteRepositoryDao.class).getObjects().iterator().next();
			final ModificationDao modificationDao = transaction.getDao(ModificationDao.class);
			final Collection<Modification> modifications = modificationDao.getModifications(remoteRepository);
			assertThat(modifications).hasSize(modificationCount);
			System.out.println("*** Accessing fromPath and toPath ***");
			for (final Modification modification : modifications) {
				if (modification instanceof CopyModification) {
					final CopyModification copyModification = (CopyModification)modification;
					System.out.println(String.format("%s => %s",
							copyModification.getFromPath(),
							copyModification.getToPath()));
				}
			}
			transaction.commit();
		} finally {
			transaction.rollbackIfActive();
		}

		localRepoManager.close();
	}
}
