package co.codewizards.cloudstore.ls.core.dto;

import co.codewizards.cloudstore.core.dto.RepositoryDto;
import co.codewizards.cloudstore.core.util.Base64Url;

public class RemoteRepositoryDto extends RepositoryDto {

	private String remoteRoot;

	public String getRemoteRoot() {
		return remoteRoot;
	}
	public void setRemoteRoot(String remoteRoot) {
		this.remoteRoot = remoteRoot;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "[repositoryId=" + getRepositoryId()
				+ ", publicKey=" + (getPublicKey() == null ? null : Base64Url.encodeBase64ToString(getPublicKey()))
				+ ", revision=" + getRevision()
				+ ", remoteRoot=" + remoteRoot
				+ "]";
	}
}
