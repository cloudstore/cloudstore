package co.codewizards.cloudstore.core.dto;

import java.util.UUID;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class RepositoryDTO {

	private UUID repositoryId;

	private byte[] publicKey;

	private long revision = -1;

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

	public long getRevision() {
		return revision;
	}
	public void setRevision(long revision) {
		this.revision = revision;
	}
}
