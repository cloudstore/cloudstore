package co.codewizards.cloudstore.core.persistence;

import java.io.File;

import javax.jdo.annotations.Discriminator;
import javax.jdo.annotations.DiscriminatorStrategy;
import javax.jdo.annotations.Index;
import javax.jdo.annotations.Indices;
import javax.jdo.annotations.Inheritance;
import javax.jdo.annotations.InheritanceStrategy;
import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.Queries;
import javax.jdo.annotations.Query;

import co.codewizards.cloudstore.core.dto.EntityID;

@PersistenceCapable
@Inheritance(strategy=InheritanceStrategy.SUPERCLASS_TABLE)
@Discriminator(strategy=DiscriminatorStrategy.VALUE_MAP, value="NormalFile")
@Indices({
	@Index(name="NormalFile_sha1_length", members={"sha1", "length"})
})
@Queries({
	@Query(name="getNormalFiles_sha1_length", value="SELECT WHERE this.sha1 == :sha1 && this.length == :length")
})
public class NormalFile extends RepoFile {

	private long length;

	@Persistent(nullValue=NullValue.EXCEPTION)
	private String sha1;

	private boolean inProgress;

	private String lastSyncFromRepositoryID;

	/**
	 * Gets the file size in bytes.
	 * <p>
	 * It reflects the {@link File#length() File.length} property.
	 * @return the file size in bytes. <code>0</code>, if this is a directory.
	 */
	public long getLength() {
		return length;
	}
	public void setLength(long size) {
		this.length = size;
	}
	/**
	 * Gets the <a href="http://en.wikipedia.org/wiki/SHA-1">SHA-1</a> of the file.
	 * @return the <a href="http://en.wikipedia.org/wiki/SHA-1">SHA-1</a> of the file.
	 */
	public String getSha1() {
		return sha1;
	}
	public void setSha1(String sha) {
		this.sha1 = sha;
	}

	/**
	 * Is this file in progress of being synced?
	 * <p>
	 * If yes, it is ignored in change-sets in order to prevent inconsistent data to propagate further.
	 * <p>
	 * TODO We should later implement a mechanism that parks all modifications locally (not in the DB, but in the
	 * meta-directory) before applying them to the file in one transaction.
	 * @return <code>true</code>, if it is currently in progress of being synced; <code>false</code> otherwise.
	 */
	public boolean isInProgress() {
		return inProgress;
	}
	public void setInProgress(boolean inProgress) {
		this.inProgress = inProgress;
	}

	public EntityID getLastSyncFromRepositoryID() {
		return lastSyncFromRepositoryID == null ? null : new EntityID(lastSyncFromRepositoryID);
	}
	public void setLastSyncFromRepositoryID(EntityID repositoryID) {
		this.lastSyncFromRepositoryID = repositoryID == null ? null : repositoryID.toString();
	}
}
