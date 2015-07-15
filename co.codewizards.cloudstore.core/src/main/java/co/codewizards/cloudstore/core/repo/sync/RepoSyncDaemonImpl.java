package co.codewizards.cloudstore.core.repo.sync;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.Severity;
import co.codewizards.cloudstore.core.dto.Error;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.local.LocalRepoHelper;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerFactory;

public class RepoSyncDaemonImpl implements RepoSyncDaemon {

	private Set<RepoSyncQueueItem> syncQueue = new LinkedHashSet<>();
	private Map<UUID, RepoSyncRunner> repositoryId2SyncRunner = new HashMap<>();
	private final ExecutorService executorService;
	private Map<UUID, List<RepoSyncState>> repositoryId2SyncStates = new HashMap<>();

	private static final class Holder {
		public static final RepoSyncDaemonImpl instance = new RepoSyncDaemonImpl();
	}

	protected RepoSyncDaemonImpl() {
		executorService = Executors.newCachedThreadPool();
	}

	public static RepoSyncDaemon getInstance() {
		return Holder.instance;
	}

	@Override
	public UUID startSync(final File file) {
		assertNotNull("file", file);
		final File localRoot = LocalRepoHelper.getLocalRootContainingFile(file);
		if (localRoot == null)
			throw new IllegalArgumentException("File is not located inside a local repository: " + file);

		final UUID repositoryId;
		try (final LocalRepoManager localRepoManager = LocalRepoManagerFactory.Helper.getInstance().createLocalRepoManagerForExistingRepository(localRoot);) {
			repositoryId = localRepoManager.getRepositoryId();
		}

		final RepoSyncQueueItem repoSyncQueueItem = new RepoSyncQueueItem(repositoryId, localRoot);
		enqueue(repoSyncQueueItem);
		startSyncWithNextSyncQueueItem(repositoryId);
		return repositoryId;
	}

	private synchronized void enqueue(final RepoSyncQueueItem repoSyncQueueItem) {
		syncQueue.add(repoSyncQueueItem);
	}

	private synchronized void startSyncWithNextSyncQueueItem(final UUID repositoryId) {
		assertNotNull("repositoryId", repositoryId);
		if (!repositoryId2SyncRunner.containsKey(repositoryId)) {
			final RepoSyncQueueItem nextSyncQueueItem = pollSyncQueueItem(repositoryId);
			if (nextSyncQueueItem != null) {
				final RepoSyncRunner repoSyncRunner = new RepoSyncRunner(nextSyncQueueItem);
				repositoryId2SyncRunner.put(nextSyncQueueItem.repositoryId, repoSyncRunner);
				submitToExecutorService(nextSyncQueueItem);
			}
		}
	}

	private void submitToExecutorService(final RepoSyncQueueItem repoSyncQueueItem) {
		final RepoSyncRunner repoSyncRunner = new RepoSyncRunner(repoSyncQueueItem);
		synchronized (this) {
			repositoryId2SyncRunner.put(repoSyncQueueItem.repositoryId, repoSyncRunner);
		}
		executorService.submit(new WrapperRunnable(repoSyncRunner));
	}

	private class WrapperRunnable implements Runnable {
		private final Logger logger = LoggerFactory.getLogger(RepoSyncDaemonImpl.WrapperRunnable.class);

		private final UUID repositoryId;
		private final RepoSyncRunner repoSyncRunner;

		public WrapperRunnable(final RepoSyncRunner repoSyncRunner) {
			this.repoSyncRunner = assertNotNull("repoSyncRunner", repoSyncRunner);
			this.repositoryId = repoSyncRunner.getSyncQueueItem().repositoryId;
		}

		@Override
		public void run() {
			try {
				repoSyncRunner.run();
				registerSyncSuccess(repoSyncRunner);
			} catch (final Throwable x) {
				logger.error("run: " + x, x);
				registerSyncError(repoSyncRunner, x);
			}
			synchronized (RepoSyncDaemonImpl.this) {
				final RepoSyncRunner removed = repositoryId2SyncRunner.remove(repositoryId);
				if (removed != repoSyncRunner)
					logger.error("run: removed != repoSyncRunner");

				startSyncWithNextSyncQueueItem(repositoryId);
			}
		}
	}

	private synchronized void registerSyncSuccess(final RepoSyncRunner repoSyncRunner) {
		assertNotNull("repoSyncRunner", repoSyncRunner);

		final UUID localRepositoryId = repoSyncRunner.getSyncQueueItem().repositoryId;
		List<RepoSyncState> list = repositoryId2SyncStates.get(localRepositoryId);
		if (list == null) {
			list = new ArrayList<>(1);
			repositoryId2SyncStates.put(localRepositoryId, list);
		}
		final File localRoot = repoSyncRunner.getSyncQueueItem().localRoot;
		for (final Map.Entry<UUID, URL> me : repoSyncRunner.getRemoteRepositoryId2RemoteRootMap().entrySet()) {
			final UUID remoteRepositoryId = me.getKey();
			final URL remoteRoot = me.getValue();
			list.add(new RepoSyncState(localRepositoryId, remoteRepositoryId, localRoot, remoteRoot,
					Severity.INFO, "Sync OK.", null));
		}
	}

	private synchronized void registerSyncError(final RepoSyncRunner repoSyncRunner, final Throwable exception) {
		assertNotNull("repoSyncRunner", repoSyncRunner);
		assertNotNull("exception", exception);

		final UUID localRepositoryId = repoSyncRunner.getSyncQueueItem().repositoryId;
		List<RepoSyncState> list = repositoryId2SyncStates.get(localRepositoryId);
		if (list == null) {
			list = new ArrayList<>(1);
			repositoryId2SyncStates.put(localRepositoryId, list);
		}
		final File localRoot = repoSyncRunner.getSyncQueueItem().localRoot;
		UUID remoteRepositoryId = repoSyncRunner.getRemoteRepositoryId();
		URL remoteRoot = repoSyncRunner.getRemoteRoot();
		if (remoteRepositoryId != null && remoteRoot != null)
			list.add(new RepoSyncState(localRepositoryId, remoteRepositoryId, localRoot, remoteRoot,
					Severity.ERROR, exception.getMessage(), new Error(exception)));
		else {
			for (Map.Entry<UUID, URL> me : repoSyncRunner.getRemoteRepositoryId2RemoteRootMap().entrySet()) {
				remoteRepositoryId = me.getKey();
				remoteRoot = me.getValue();
				list.add(new RepoSyncState(localRepositoryId, remoteRepositoryId, localRoot, remoteRoot,
						Severity.ERROR, exception.getMessage(), new Error(exception)));
			}
		}
	}

	private synchronized RepoSyncQueueItem pollSyncQueueItem(UUID repositoryId) {
		assertNotNull("repositoryId", repositoryId);
		for (Iterator<RepoSyncQueueItem> it = syncQueue.iterator(); it.hasNext(); ) {
			final RepoSyncQueueItem repoSyncQueueItem = it.next();
			if (repositoryId.equals(repoSyncQueueItem.repositoryId)) {
				it.remove();
				return repoSyncQueueItem;
			}
		}
		return null;
	}

	@Override
	public synchronized List<RepoSyncState> getSyncStates(final UUID localRepositoryId) {
		assertNotNull("repositoryId", localRepositoryId);
		final List<RepoSyncState> list = repositoryId2SyncStates.get(localRepositoryId);
		if (list == null)
			return Collections.emptyList();
		else
			return Collections.unmodifiableList(new ArrayList<>(list));
	}

	@Override
	public synchronized void removeSyncStates(final Collection<RepoSyncState> repoSyncStates) {
		assertNotNull("repoSyncStates", repoSyncStates);
		for (final RepoSyncState repoSyncState : repoSyncStates) {
			final UUID repositoryId = repoSyncState.getLocalRepositoryId();
			final List<RepoSyncState> list = repositoryId2SyncStates.get(repositoryId);
			if (list != null)
				list.remove(repoSyncState);
		}
	}

	@Override
	public void shutdown() {
		executorService.shutdown();
	}

	@Override
	public void shutdownNow() {
		executorService.shutdownNow();
	}
}
