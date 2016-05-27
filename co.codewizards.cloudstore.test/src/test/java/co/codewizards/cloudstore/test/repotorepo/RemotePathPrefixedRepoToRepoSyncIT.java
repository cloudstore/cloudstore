package co.codewizards.cloudstore.test.repotorepo;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemotePathPrefixedRepoToRepoSyncIT extends AbstractRepoToRepoSyncIT
{
	private static final Logger logger = LoggerFactory.getLogger(RemotePathPrefixedRepoToRepoSyncIT.class);

	@Test
	public void syncFromRemoteToLocalWithRemotePathPrefix() throws Exception {
		remotePathPrefix = "/2";
		syncFromRemoteToLocal();
	}

	@Test
	public void syncFromRemoteToLocalWithRemotePathPrefix_specialChar() throws Exception {
		remotePathPrefix = "/#4";
		syncFromRemoteToLocal();
	}
	@Test
	public void syncFromRemoteToLocalWithRemotePathPrefix_specialChar2() throws Exception {
		remotePathPrefix = "/5#";
		syncFromRemoteToLocal();
	}

	@Test
	public void syncMovedFileWithRemotePathPrefix() throws Exception {
		remotePathPrefix = "/2";
		syncMovedFile();
	}

	@Test
	public void syncMovedFileToNewDirWithRemotePathPrefix() throws Exception {
		remotePathPrefix = "/2";
		syncMovedFileToNewDir();
	}
}
