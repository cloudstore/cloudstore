package co.codewizards.cloudstore.test.repotorepo;

import org.junit.Test;

/**
 * In contrast to the {@code RepoToRepoSyncTest}, this test (and all other tests in this package)
 * uses the REST transport.
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public class BasicRepoToRepoSyncIT extends AbstractRepoToRepoSyncIT
{
	@Override
	@Test
	public void syncFromRemoteToLocal() throws Exception {
		super.syncFromRemoteToLocal();
	}

	@Override
	@Test
	public void syncFromLocalToRemote() throws Exception {
		super.syncFromLocalToRemote();
	}

	@Override
	@Test
	public void syncMovedFile() throws Exception {
		super.syncMovedFile();
	}

	@Override
	@Test
	public void syncMovedFileToNewDir() throws Exception {
		super.syncMovedFileToNewDir();
	}
}
