package co.codewizards.cloudstore.core.persistence;

import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;

import co.codewizards.cloudstore.core.dto.EntityID;

@PersistenceCapable
public class RemoteRepositoryRequest extends Entity {
	@Persistent(nullValue=NullValue.EXCEPTION)
	private byte[] publicKey;

	public RemoteRepositoryRequest() { }

	public RemoteRepositoryRequest(EntityID repositoryID) {
		super(repositoryID);
	}

	public EntityID getRepositoryID() {
		return getEntityID();
	}

	public byte[] getPublicKey() {
		return publicKey;
	}

	public void setPublicKey(byte[] publicKey) {
		this.publicKey = publicKey;
	}
}
