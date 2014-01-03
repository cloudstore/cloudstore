package co.codewizards.cloudstore.shared.dto;

import co.codewizards.cloudstore.shared.persistence.LocalRepository;

/**
 * Request for a change set.
 * <p>
 * All changes <code>after</code> the specified {@link #getRevision() revision} are expected in the
 * {@link ChangeSetResponse}.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public class ChangeSetRequest {

	private long revision;

	/**
	 * Get the last revision that was synchronized.
	 * <p>
	 * This specifies the {@link LocalRepository#getRevision() LocalRepository.revision} on the server side.
	 * The server is expected to send all changes that were done after this revision. Most importantly, this means
	 * to send all {@link RepoFileDTO}s whose corresponding
	 * {@link co.codewizards.cloudstore.shared.persistence.RepoFile#getLocalRevision() RepoFile.localRevision} have
	 * a value greater than (&gt) than the value specified here.
	 * @return the last revision that was synchronized.
	 */
	public long getRevision() {
		return revision;
	}

	public void setRevision(long localRevision) {
		this.revision = localRevision;
	}

}
