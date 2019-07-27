package co.codewizards.cloudstore.core.repo.sync;

import static java.util.Objects.*;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.Severity;
import co.codewizards.cloudstore.core.config.Config;
import co.codewizards.cloudstore.core.config.ConfigImpl;
import co.codewizards.cloudstore.core.dto.Error;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.local.LocalRepoHelper;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerFactory;

public class RepoSyncDaemonImpl implements RepoSyncDaemon {
	private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

	private Set<RepoSyncQueueItem> syncQueue = new LinkedHashSet<>();
	private Map<UUID, RepoSyncRunner> repositoryId2SyncRunner = new HashMap<>();
	private final ExecutorService executorService;
	private Map<UUID, Set<RepoSyncActivity>> repositoryId2SyncActivities = new HashMap<>();
	private Map<UUID, List<RepoSyncState>> repositoryId2SyncStates = new HashMap<>();
	private static final AtomicInteger threadGroupIndex = new AtomicInteger();
	private final AtomicInteger threadIndex = new AtomicInteger();

	private static final class Holder {
		public static final RepoSyncDaemonImpl instance = new RepoSyncDaemonImpl();
	}

	protected RepoSyncDaemonImpl() {
		final int tgi = threadGroupIndex.getAndIncrement();
		final ThreadGroup threadGroup = new ThreadGroup("RepoSyncDaemonThreadGroup_" + tgi);
		executorService = Executors.newCachedThreadPool(new ThreadFactory() {
			@Override
			public Thread newThread(Runnable r) {
				return new Thread(threadGroup, r, "RepoSyncDaemonThread_" + tgi + "_" + threadIndex.getAndIncrement());
			}
		});
	}

	public static RepoSyncDaemon getInstance() {
		return Holder.instance;
	}

	@Override
	public UUID startSync(final File file) {
		requireNonNull(file, "file");
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
		updateActivities(repositoryId);
		return repositoryId;
	}

	private synchronized void enqueue(final RepoSyncQueueItem repoSyncQueueItem) {
		syncQueue.add(repoSyncQueueItem);
	}

	private synchronized void startSyncWithNextSyncQueueItem(final UUID repositoryId) {
		requireNonNull(repositoryId, "repositoryId");
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
			this.repoSyncRunner = requireNonNull(repoSyncRunner, "repoSyncRunner");
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
			updateActivities(repositoryId);
		}
	}

	private void registerSyncSuccess(final RepoSyncRunner repoSyncRunner) {
		requireNonNull(repoSyncRunner, "repoSyncRunner");

		final List<RepoSyncState> statesAdded = new ArrayList<RepoSyncState>();
		final List<RepoSyncState> statesRemoved;
		final UUID localRepositoryId = repoSyncRunner.getSyncQueueItem().repositoryId;
		final File localRoot = repoSyncRunner.getSyncQueueItem().localRoot;
		synchronized (this) {
			final List<RepoSyncState> list = _getRepoSyncStates(localRepositoryId);
			for (final Map.Entry<UUID, URL> me : repoSyncRunner.getRemoteRepositoryId2RemoteRootMap().entrySet()) {
				final UUID remoteRepositoryId = me.getKey();
				final URL remoteRoot = me.getValue();
				final RepoSyncState state = new RepoSyncState(localRepositoryId, remoteRepositoryId, localRoot, remoteRoot,
						Severity.INFO, "Sync OK.", null,
						repoSyncRunner.getSyncStarted(), repoSyncRunner.getSyncFinished());
				list.add(state);
				statesAdded.add(state);
			}
			statesRemoved = evictOldStates(localRepositoryId, localRoot);
		}

		firePropertyChange(PropertyEnum.states_added, null, Collections.unmodifiableList(statesAdded));

		if (! statesRemoved.isEmpty())
			firePropertyChange(PropertyEnum.states_removed, null, Collections.unmodifiableList(statesRemoved));

		firePropertyChange(PropertyEnum.states, null, getStates(localRepositoryId));
	}

	private void registerSyncError(final RepoSyncRunner repoSyncRunner, final Throwable exception) {
		requireNonNull(repoSyncRunner, "repoSyncRunner");
		requireNonNull(exception, "exception");

		final List<RepoSyncState> statesAdded = new ArrayList<RepoSyncState>();
		final List<RepoSyncState> statesRemoved;
		final UUID localRepositoryId = repoSyncRunner.getSyncQueueItem().repositoryId;
		final File localRoot = repoSyncRunner.getSyncQueueItem().localRoot;
		synchronized (this) {
			final List<RepoSyncState> list = _getRepoSyncStates(localRepositoryId);
			UUID remoteRepositoryId = repoSyncRunner.getRemoteRepositoryId();
			URL remoteRoot = repoSyncRunner.getRemoteRoot();
			final RepoSyncState state = new RepoSyncState(localRepositoryId, remoteRepositoryId, localRoot, remoteRoot,
					Severity.ERROR, exception.getMessage(), new Error(exception),
					repoSyncRunner.getSyncStarted(), repoSyncRunner.getSyncFinished());
			if (remoteRepositoryId != null && remoteRoot != null) {
				list.add(state);
				statesAdded.add(state);
			}
			else {
				for (Map.Entry<UUID, URL> me : repoSyncRunner.getRemoteRepositoryId2RemoteRootMap().entrySet()) {
					remoteRepositoryId = me.getKey();
					remoteRoot = me.getValue();
					list.add(state);
					statesAdded.add(state);
				}
			}
			statesRemoved = evictOldStates(localRepositoryId, localRoot);
		}

		firePropertyChange(PropertyEnum.states_added, null, Collections.unmodifiableList(statesAdded));

		if (! statesRemoved.isEmpty())
			firePropertyChange(PropertyEnum.states_removed, null, Collections.unmodifiableList(statesRemoved));

		firePropertyChange(PropertyEnum.states, null, getStates(localRepositoryId));
	}

	private List<RepoSyncState> _getRepoSyncStates(final UUID localRepositoryId) {
		List<RepoSyncState> list = repositoryId2SyncStates.get(localRepositoryId);
		if (list == null) {
			list = new LinkedList<>();
			repositoryId2SyncStates.put(localRepositoryId, list);
		}
		return list;
	}

	private synchronized List<RepoSyncState> evictOldStates(final UUID localRepositoryId, final File localRoot) {
		requireNonNull(localRepositoryId, "localRepositoryId");
		requireNonNull(localRoot, "localRoot");
		final List<RepoSyncState> evicted = new ArrayList<RepoSyncState>();
		final Config config = ConfigImpl.getInstanceForDirectory(localRoot);
		final int syncStatesMaxSize = config.getPropertyAsPositiveOrZeroInt(CONFIG_KEY_SYNC_STATES_MAX_SIZE, DEFAULT_SYNC_STATES_MAX_SIZE);
		final List<RepoSyncState> list = repositoryId2SyncStates.get(localRepositoryId);
		if (list != null) {
			// Note: This implementation is not very efficient, but the list usually has a size of only a few
			// entries - rarely ever more than a few dozen. Thus, this algorithm is certainly fast enough ;-)
			for (final Iterator<RepoSyncState> it = list.iterator(); it.hasNext();) {
				final RepoSyncState repoSyncState = it.next();
				if (getSyncStatesSizeForServerRepositoryId(list, repoSyncState.getServerRepositoryId()) > syncStatesMaxSize) {
					evicted.add(repoSyncState);
					it.remove();
				}
			}
		}
		return evicted;
	}

	private int getSyncStatesSizeForServerRepositoryId(final List<RepoSyncState> repoSyncStates, final UUID serverRepositoryId) {
		requireNonNull(serverRepositoryId, "serverRepositoryId");
		int result = 0;
		for (RepoSyncState repoSyncState : repoSyncStates) {
			if (serverRepositoryId.equals(repoSyncState.getServerRepositoryId()))
				++result;
		}
		return result;
	}

	private synchronized RepoSyncQueueItem pollSyncQueueItem(UUID repositoryId) {
		requireNonNull(repositoryId, "repositoryId");
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
	public synchronized List<RepoSyncState> getStates(final UUID localRepositoryId) {
		requireNonNull(localRepositoryId, "localRepositoryId");
		final List<RepoSyncState> list = repositoryId2SyncStates.get(localRepositoryId);
		if (list == null)
			return Collections.emptyList();
		else
			return Collections.unmodifiableList(new ArrayList<>(list));
	}

	@Override
	public synchronized Set<RepoSyncActivity> getActivities(final UUID localRepositoryId) {
		requireNonNull(localRepositoryId, "localRepositoryId");
		final Set<RepoSyncActivity> activities = repositoryId2SyncActivities.get(localRepositoryId);
		if (activities == null)
			return Collections.emptySet();

		return Collections.unmodifiableSet(new HashSet<RepoSyncActivity>(activities));
	}

	private void updateActivities(final UUID localRepositoryId) {
		requireNonNull(localRepositoryId, "localRepositoryId");

		final List<RepoSyncActivity> activitiesAdded = new ArrayList<RepoSyncActivity>();
		final List<RepoSyncActivity> activitiesRemoved = new ArrayList<RepoSyncActivity>();

		synchronized (this) {
			Set<RepoSyncActivity> activities = repositoryId2SyncActivities.get(localRepositoryId);
			if (activities == null) {
				activities = new HashSet<RepoSyncActivity>(2);
				repositoryId2SyncActivities.put(localRepositoryId, activities);
			}

			final RepoSyncRunner repoSyncRunner = repositoryId2SyncRunner.get(localRepositoryId);
			if (repoSyncRunner == null) {
				final List<RepoSyncActivity> activitiesStale = _findActivities(localRepositoryId, RepoSyncActivityType.IN_PROGRESS);
				activitiesRemoved.addAll(activitiesStale);
				activities.removeAll(activitiesStale);
			}
			else {
				final RepoSyncActivity activity = new RepoSyncActivity(
						repoSyncRunner.getSyncQueueItem().repositoryId,
						repoSyncRunner.getSyncQueueItem().localRoot, RepoSyncActivityType.IN_PROGRESS);

				if (activities.add(activity))
					activitiesAdded.add(activity);
			}

			final List<RepoSyncQueueItem> queueItems = _findQueueItems(localRepositoryId);
			if (queueItems.isEmpty()) {
				final List<RepoSyncActivity> activitiesStale = _findActivities(localRepositoryId, RepoSyncActivityType.QUEUED);
				activitiesRemoved.addAll(activitiesStale);
				activities.removeAll(activitiesStale);
			}
			else {
				final RepoSyncActivity activity = new RepoSyncActivity(
						repoSyncRunner.getSyncQueueItem().repositoryId,
						repoSyncRunner.getSyncQueueItem().localRoot, RepoSyncActivityType.QUEUED);

				if (activities.add(activity))
					activitiesAdded.add(activity);
			}
		}

		if (! activitiesRemoved.isEmpty())
			firePropertyChange(PropertyEnum.activities_removed, null, activitiesRemoved);

		if (! activitiesAdded.isEmpty())
			firePropertyChange(PropertyEnum.activities_added, null, activitiesAdded);

		if (! activitiesAdded.isEmpty() || ! activitiesRemoved.isEmpty())
			firePropertyChange(PropertyEnum.activities, null, getActivities(localRepositoryId));
	}

	private synchronized List<RepoSyncActivity> _findActivities(final UUID localRepositoryId, final RepoSyncActivityType activityType) {
		requireNonNull(localRepositoryId, "localRepositoryId");
		final Set<RepoSyncActivity> activities = repositoryId2SyncActivities.get(localRepositoryId);
		if (activities == null)
			return Collections.emptyList();

		final List<RepoSyncActivity> result = new ArrayList<>(1);
		for (RepoSyncActivity activity : activities) {
			if (activity.getActivityType() == activityType)
				result.add(activity);
		}

		return Collections.unmodifiableList(result);
	}

	private synchronized List<RepoSyncQueueItem> _findQueueItems(final UUID localRepositoryId) {
		requireNonNull(localRepositoryId, "localRepositoryId");
		final List<RepoSyncQueueItem> result = new ArrayList<RepoSyncQueueItem>(2);
		for (final RepoSyncQueueItem queueItem : syncQueue) {
			if (localRepositoryId.equals(queueItem.repositoryId))
				result.add(queueItem);
		}
		return Collections.unmodifiableList(result);
	}

	@Override
	public void shutdown() {
		executorService.shutdown();
	}

	@Override
	public void shutdownNow() {
		executorService.shutdownNow();
	}

	@Override
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		propertyChangeSupport.addPropertyChangeListener(listener);
	}

	@Override
	public void addPropertyChangeListener(Property property, PropertyChangeListener listener) {
		propertyChangeSupport.addPropertyChangeListener(property.name(), listener);
	}

	@Override
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		propertyChangeSupport.removePropertyChangeListener(listener);
	}

	@Override
	public void removePropertyChangeListener(Property property, PropertyChangeListener listener) {
		propertyChangeSupport.removePropertyChangeListener(property.name(), listener);
	}

	protected void firePropertyChange(Property property, Object oldValue, Object newValue) {
		propertyChangeSupport.firePropertyChange(property.name(), oldValue, newValue);
	}
}
