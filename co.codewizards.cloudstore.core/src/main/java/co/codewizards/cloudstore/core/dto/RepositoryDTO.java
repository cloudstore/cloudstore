package co.codewizards.cloudstore.core.dto;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class RepositoryDTO {

	private EntityID entityID;

	private byte[] publicKey;

	private long revision = -1;

	public EntityID getEntityID() {
		return entityID;
	}
	public void setEntityID(EntityID entityID) {
		this.entityID = entityID;
	}

	public byte[] getPublicKey() {
		return publicKey;
	}
	public void setPublicKey(byte[] publicKey) {
		this.publicKey = publicKey;
	}

	public long getRevision() {
		return revision;
	}
	public void setRevision(long revision) {
		this.revision = revision;
	}
}
