package co.codewizards.cloudstore.core.dto;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ModificationDTO {

	private EntityID entityID;
	private long localRevision;

	public EntityID getEntityID() {
		return entityID;
	}
	public void setEntityID(EntityID entityID) {
		this.entityID = entityID;
	}

	public long getLocalRevision() {
		return localRevision;
	}
	public void setLocalRevision(long localRevision) {
		this.localRevision = localRevision;
	}

}
