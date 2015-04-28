package co.codewizards.cloudstore.ls.core.dto;

import java.util.Date;
import java.util.UUID;

public class RemoteRepositoryRequestDto {

	private UUID repositoryId;
	private byte[] publicKey;
	private Date created;
	private Date changed;

	public UUID getRepositoryId() {
		return repositoryId;
	}

	public void setRepositoryId(UUID repositoryId) {
		this.repositoryId = repositoryId;
	}

	public byte[] getPublicKey() {
		return publicKey;
	}

	public void setPublicKey(byte[] publicKey) {
		this.publicKey = publicKey;
	}

	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	public Date getChanged() {
		return changed;
	}

	public void setChanged(Date changed) {
		this.changed = changed;
	}
}
