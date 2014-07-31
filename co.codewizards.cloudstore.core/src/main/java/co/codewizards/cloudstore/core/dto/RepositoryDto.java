package co.codewizards.cloudstore.core.dto;

import java.util.UUID;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Dto for {@code LocalRepository} and {@code RemoteRepository}.
 * <p>
 * <b>Important:</b> This object can be read anonymously without authentication! It therefore
 * does currently not contain any sensitive information.
 * <p>
 * Future refactorings must take this into account and never add any secret data here! The world
 * can read it! See {@code RepositoryDtoService}.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
@XmlRootElement
public class RepositoryDto {

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
