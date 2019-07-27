package co.codewizards.cloudstore.core.repo.sync;

import static java.util.Objects.*;

import java.net.URL;
import java.util.Date;
import java.util.UUID;

import co.codewizards.cloudstore.core.Severity;
import co.codewizards.cloudstore.core.dto.Error;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.sync.SyncState;

@SuppressWarnings("serial")
public class RepoSyncState extends SyncState {

	private final UUID localRepositoryId;
	private final UUID serverRepositoryId;
	private final File localRoot;

	public RepoSyncState(UUID localRepositoryId, UUID serverRepositoryId, File localRoot, URL url, Severity severity, String message, Error error, Date syncStarted, Date syncFinished) {
		super(url, severity, message, error, syncStarted, syncFinished);
		this.localRepositoryId = requireNonNull(localRepositoryId, "localRepositoryId");
		this.serverRepositoryId = requireNonNull(serverRepositoryId, "serverRepositoryId");
		this.localRoot = requireNonNull(localRoot, "localRoot");
	}

	public UUID getLocalRepositoryId() {
		return localRepositoryId;
	}

	public UUID getServerRepositoryId() {
		return serverRepositoryId;
	}

	public File getLocalRoot() {
		return localRoot;
	}
}
