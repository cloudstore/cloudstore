package co.codewizards.cloudstore.core.repo.sync;

import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import co.codewizards.cloudstore.core.bean.Bean;
import co.codewizards.cloudstore.core.bean.PropertyBase;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.sync.SyncState;

public interface RepoSyncDaemon extends Bean<RepoSyncDaemon.Property> {
	public static interface Property extends PropertyBase {
	}

	public static enum PropertyEnum implements Property {
		activities,
		activities_added,
		activities_removed,

		states,
		states_added,
		states_removed
	}

	/**
	 * The maximum number of {@link SyncState} elements to keep for each unique combination of
	 * {@code localRepositoryId} and {@code remoteRepositoryId}.
	 * <p>
	 * The default value is {@link #DEFAULT_SYNC_STATES_MAX_SIZE}.
	 * <p>
	 * Example:
	 * If this value is set to 2 and a certain local repository is connected to 3 remote repositories,
	 * there are 3 unique combinations of local/remote-repository-id and therefore 6 sync-states are kept.
	 * These sync-states are the 2 newest ones for each pair of local+remote repo.
	 */
	String CONFIG_KEY_SYNC_STATES_MAX_SIZE = "repoSyncDaemon.syncStates.maxSize";
	int DEFAULT_SYNC_STATES_MAX_SIZE = 1;

	UUID startSync(File file);

	void shutdown();

	void shutdownNow();

	/**
	 * Gets the sync-states of the local repository identified by the given {@code localRepositoryId}.
	 * <p>
	 * The sync-states are sorted according to the time they are enlisted with the <b>newest entries last</b>
	 * (i.e. the oldest first).
	 * <p>
	 * Please note that these states describe only syncs that are complete (either successful or failed).
	 * To determine whether a sync is currently queued or in progress, please use {@link #getActivities(UUID)}.
	 * <p>
	 * @param localRepositoryId the identifier of the local repository. Must not be <code>null</code>.
	 * @return the sync-states. Never <code>null</code> (but maybe empty). This result is read-only and
	 * a detached copy of the internal state (not backed!).
	 */
	List<RepoSyncState> getStates(UUID localRepositoryId);

	/**
	 * Gets all {@link RepoSyncActivity}-s for the local repository identified by the given {@code localRepositoryId}.
	 * @param localRepositoryId the identifier of the local repository. Must not be <code>null</code>.
	 * @return the activities. Never <code>null</code> (but maybe empty). This result is read-only and
	 * a detached copy of the internal state (not backed!).
	 */
	Set<RepoSyncActivity> getActivities(final UUID localRepositoryId);

	@Override
	void addPropertyChangeListener(PropertyChangeListener listener);

	@Override
	void addPropertyChangeListener(Property property, PropertyChangeListener listener);

	@Override
	void removePropertyChangeListener(PropertyChangeListener listener);

	@Override
	void removePropertyChangeListener(Property property, PropertyChangeListener listener);
}