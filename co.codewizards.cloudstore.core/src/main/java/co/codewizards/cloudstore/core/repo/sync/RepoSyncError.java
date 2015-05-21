package co.codewizards.cloudstore.core.repo.sync;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

import co.codewizards.cloudstore.core.dto.Error;

public class RepoSyncError implements Serializable {
	private static final long serialVersionUID = 1L;

	private final Date created = new Date();
	private final UUID repositoryId;
	private final Error error;

	public RepoSyncError(final UUID repositoryId, final Error error) {
		this.repositoryId = assertNotNull("repositoryId", repositoryId);
		this.error = assertNotNull("error", error);
	}

	public Date getCreated() {
		return created;
	}

	public UUID getRepositoryId() {
		return repositoryId;
	}

	public Error getError() {
		return error;
	}
}
