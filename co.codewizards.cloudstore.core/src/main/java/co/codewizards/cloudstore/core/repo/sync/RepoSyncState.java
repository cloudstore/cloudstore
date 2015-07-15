package co.codewizards.cloudstore.core.repo.sync;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.net.URL;
import java.util.UUID;

import co.codewizards.cloudstore.core.Severity;
import co.codewizards.cloudstore.core.dto.Error;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.sync.SyncState;

public class RepoSyncState extends SyncState {
	private static final long serialVersionUID = 1L;

	private final UUID localRepositoryId;
	private final UUID serverRepositoryId;
	private final File localRoot;

	public RepoSyncState(UUID localRepositoryId, UUID serverRepositoryId, File localRoot, URL url, Severity severity, String message, Error error) {
		super(url, severity, message, error);
		this.localRepositoryId = assertNotNull("localRepositoryId", localRepositoryId);
		this.serverRepositoryId = assertNotNull("serverRepositoryId", serverRepositoryId);
		this.localRoot = assertNotNull("localRoot", localRoot);
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
