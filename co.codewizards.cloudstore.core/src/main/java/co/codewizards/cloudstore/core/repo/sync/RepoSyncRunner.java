package co.codewizards.cloudstore.core.repo.sync;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.progress.LoggerProgressMonitor;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerFactory;

class RepoSyncRunner implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(RepoSyncRunner.class);

	private final RepoSyncQueueItem repoSyncQueueItem;

	public RepoSyncRunner(final RepoSyncQueueItem repoSyncQueueItem) {
		this.repoSyncQueueItem = assertNotNull("repoSyncQueueItem", repoSyncQueueItem);
	}

	public RepoSyncQueueItem getSyncQueueItem() {
		return repoSyncQueueItem;
	}

	@Override
	public void run() {
		final List<URL> remoteRoots = new ArrayList<URL>();
		try (final LocalRepoManager localRepoManager = LocalRepoManagerFactory.Helper.getInstance().createLocalRepoManagerForExistingRepository(repoSyncQueueItem.localRoot);) {
			for (final URL url : localRepoManager.getRemoteRepositoryId2RemoteRootMap().values())
				remoteRoots.add(url);
		}

		for (final URL remoteRoot : remoteRoots) {
			try (RepoToRepoSync repoToRepoSync = RepoToRepoSync.create(repoSyncQueueItem.localRoot, remoteRoot);) {
				repoToRepoSync.sync(new LoggerProgressMonitor(logger));
			}
		}
	}
}
