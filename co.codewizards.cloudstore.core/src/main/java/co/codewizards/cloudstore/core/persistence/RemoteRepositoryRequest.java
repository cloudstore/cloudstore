package co.codewizards.cloudstore.core.persistence;

import javax.jdo.annotations.PersistenceCapable;

import co.codewizards.cloudstore.core.dto.EntityID;

@PersistenceCapable
public class RemoteRepositoryRequest extends Entity {

	public RemoteRepositoryRequest() { }

	public RemoteRepositoryRequest(EntityID repositoryID) {
		super(repositoryID);
	}

	public EntityID getRepositoryID() {
		return getEntityID();
	}

}
