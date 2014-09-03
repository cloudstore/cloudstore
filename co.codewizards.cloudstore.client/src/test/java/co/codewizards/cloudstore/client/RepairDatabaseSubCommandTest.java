package co.codewizards.cloudstore.client;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;

public class RepairDatabaseSubCommandTest extends AbstractTest {

	@BeforeClass
	public static void beforeClass() {
		System.setProperty(LocalRepoManager.SYSTEM_PROPERTY_CLOSE_DEFERRED_MILLIS, "0");
	}

	@AfterClass
	public static void afterClass() {
		System.clearProperty(LocalRepoManager.SYSTEM_PROPERTY_CLOSE_DEFERRED_MILLIS);
	}

	@Test
	public void repairDatabase() throws Exception {
		File localRoot = newTestRepositoryLocalRoot("");
		new CloudStoreClient("createRepo", localRoot.getPath(), "-createDir").execute();
		new CloudStoreClient("repairDatabase", localRoot.getPath()).execute();
	}

}
