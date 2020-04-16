package co.codewizards.cloudstore.local.dbupdate;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static org.assertj.core.api.Assertions.*;

import org.junit.Test;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.util.IOUtil;
import co.codewizards.cloudstore.core.util.ZipUtil;
import co.codewizards.cloudstore.local.AbstractTest;
import co.codewizards.cloudstore.local.db.DatabaseMigrater;

public class DbUpdateTest extends AbstractTest {

	@Test
	public void updateDbVersion001() throws Exception {
		final File localRootZip = createFile(getTestRepositoryBaseDir(), "k1f118b3-kd4ta.zip");
		IOUtil.copyResource(DbUpdateTest.class, "/" + localRootZip.getName(), localRootZip);

		final File localRoot = createFile(getTestRepositoryBaseDir(), "k1f118b3-kd4ta");
		ZipUtil.unzipArchive(localRootZip, localRoot.getParentFile());
		if (! localRoot.isDirectory())
			throw new IllegalStateException("Directory does not exist: " + localRoot.getAbsolutePath());

		DatabaseMigrater.create(localRoot).createTriggerFile(); // we do *not* want to trigger the newly implemented DB-migration!

		LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(localRoot);
		assertThat(localRepoManager).isNotNull();
		localRepoManager.close();
	}

}
