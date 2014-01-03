package co.codewizards.cloudstore.core.dto;

import co.codewizards.cloudstore.core.persistence.LocalRepository;

/**
 * Request for a change set.
 * <p>
 * All changes <code>after</code> the specified {@link #getServerRevision() serverRevision} are expected in the
 * {@link ChangeSetResponse}.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public class ChangeSetRequest {

	private EntityID clientRepositoryID;

	private long serverRevision;

	/**
	 * Get the last revision that was synchronized.
	 * <p>
	 * This specifies the {@link LocalRepository#getRevision() LocalRepository.revision} on the server side.
	 * The server is expected to send all changes that were done after this serverRevision. Most importantly, this means
	 * to send all {@link RepoFileDTO}s whose corresponding
	 * {@link co.codewizards.cloudstore.core.persistence.RepoFile#getLocalRevision() RepoFile.localRevision} have
	 * a value greater than (&gt) than the value specified here.
	 * @return the last serverRevision that was synchronized.
	 */
	public long getServerRevision() {
		return serverRevision;
	}

	public void setServerRevision(long localRevision) {
		this.serverRevision = localRevision;
	}

	public EntityID getClientRepositoryID() {
		return clientRepositoryID;
	}
	public void setClientRepositoryID(EntityID clientRepositoryID) {
		this.clientRepositoryID = clientRepositoryID;
	}
}
