package co.codewizards.cloudstore.shared.dto;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class RepositoryDTO {

	private EntityID entityID;

	private long revision;

	public EntityID getEntityID() {
		return entityID;
	}
	public void setEntityID(EntityID entityID) {
		this.entityID = entityID;
	}
	public long getRevision() {
		return revision;
	}
	public void setRevision(long revision) {
		this.revision = revision;
	}

}
