package co.codewizards.cloudstore.core.repo.sync;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.progress.LoggerProgressMonitor;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerFactory;

class RepoSyncRunner implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(RepoSyncRunner.class);

	private final RepoSyncQueueItem repoSyncQueueItem;
	private Map<UUID, URL> remoteRepositoryId2RemoteRootMap = new HashMap<>(0);
	private UUID remoteRepositoryId;
	private URL remoteRoot;
	private Date syncStarted;
	private Date syncFinished;

	public RepoSyncRunner(final RepoSyncQueueItem repoSyncQueueItem) {
		this.repoSyncQueueItem = assertNotNull("repoSyncQueueItem", repoSyncQueueItem);
	}

	public RepoSyncQueueItem getSyncQueueItem() {
		return repoSyncQueueItem;
	}

	@Override
	public void run() {
		syncStarted = new Date();
		try {
			remoteRepositoryId = null;
			remoteRoot = null;
			try (final LocalRepoManager localRepoManager = LocalRepoManagerFactory.Helper.getInstance().createLocalRepoManagerForExistingRepository(repoSyncQueueItem.localRoot);) {
				remoteRepositoryId2RemoteRootMap = new HashMap<>(localRepoManager.getRemoteRepositoryId2RemoteRootMap());
			}

			for (Map.Entry<UUID, URL> me : remoteRepositoryId2RemoteRootMap.entrySet()) {
				remoteRepositoryId = me.getKey();
				remoteRoot = me.getValue();
				try (RepoToRepoSync repoToRepoSync = RepoToRepoSync.create(repoSyncQueueItem.localRoot, remoteRoot);) {
					repoToRepoSync.sync(new LoggerProgressMonitor(logger));
				}
			}
			remoteRepositoryId = null;
			remoteRoot = null;
		} finally {
			syncFinished = new Date();
		}
	}

	public UUID getRemoteRepositoryId() {
		return remoteRepositoryId;
	}

	/**
	 * Gets the current remote root URL or the last one having synced.
	 * <p>
	 * If the sync was aborted due to a failure, this reflects the URL that failed to sync. If all
	 * syncs succeeded, this is <code>null</code>.
	 * @return the current/last remote root URL or <code>null</code>.
	 */
	public URL getRemoteRoot() {
		return remoteRoot;
	}

	/**
	 * Gets all remote repository IDs and root URLs that were used in the last sync.
	 * @return all remote repository IDs and root URLs used in the last sync. Never <code>null</code>.
	 */
	public Map<UUID, URL> getRemoteRepositoryId2RemoteRootMap() {
		return remoteRepositoryId2RemoteRootMap;
	}

	public Date getSyncStarted() {
		return syncStarted;
	}

	public Date getSyncFinished() {
		return syncFinished;
	}
}
