package co.codewizards.cloudstore.core.persistence;

import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.Query;
import javax.jdo.annotations.Unique;

import co.codewizards.cloudstore.core.dto.EntityID;

@PersistenceCapable
@Unique(name="RemoteRepositoryRequest_repositoryID", members="repositoryID")
@Query(name="getRemoteRepositoryRequest_repositoryID", value="SELECT UNIQUE WHERE this.repositoryID == :repositoryID")
public class RemoteRepositoryRequest extends Entity {

	public RemoteRepositoryRequest() { }

	@Persistent(nullValue=NullValue.EXCEPTION)
	private String repositoryID;

	@Persistent(nullValue=NullValue.EXCEPTION)
	private byte[] publicKey;

	@Persistent(nullValue=NullValue.EXCEPTION)
	private String localPathPrefix;

	public EntityID getRepositoryID() {
		return repositoryID == null ? null : new EntityID(repositoryID);
	}
	public void setRepositoryID(EntityID repositoryID) {
		this.repositoryID = repositoryID == null ? null : repositoryID.toString();
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
