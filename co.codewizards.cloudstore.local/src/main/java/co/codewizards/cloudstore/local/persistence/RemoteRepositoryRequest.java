package co.codewizards.cloudstore.local.persistence;

import java.util.UUID;

import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.Queries;
import javax.jdo.annotations.Query;
import javax.jdo.annotations.Unique;

@PersistenceCapable
@Unique(name="RemoteRepositoryRequest_repositoryId", members="repositoryId")
@Queries({
	@Query(name="getRemoteRepositoryRequest_repositoryId", value="SELECT UNIQUE WHERE this.repositoryId == :repositoryId"),
	@Query(name="getRemoteRepositoryRequestsChangedBefore_changed", value="SELECT WHERE this.changed < :changed")
})
public class RemoteRepositoryRequest extends Entity {

	public RemoteRepositoryRequest() { }

	@Persistent(nullValue=NullValue.EXCEPTION)
	private String repositoryId;

	@Persistent(nullValue=NullValue.EXCEPTION)
	private byte[] publicKey;

	@Persistent(nullValue=NullValue.EXCEPTION)
	private String localPathPrefix;

	public UUID getRepositoryId() {
		return repositoryId == null ? null : UUID.fromString(repositoryId);
	}
	public void setRepositoryId(UUID repositoryId) {
		this.repositoryId = repositoryId == null ? null : repositoryId.toString();
	}

	public byte[] getPublicKey() {
		return publicKey;
	}

	public void setPublicKey(byte[] publicKey) {
		this.publicKey = publicKey;
	}

	public String getLocalPathPrefix() {
		return localPathPrefix;
	}
	public void setLocalPathPrefix(String localPathPrefix) {
		this.localPathPrefix = localPathPrefix;
	}
}
