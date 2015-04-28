package co.codewizards.cloudstore.ls.core.dto;

import co.codewizards.cloudstore.core.dto.RepositoryDto;

public class RemoteRepositoryDto extends RepositoryDto {

	private String remoteRoot;

	public String getRemoteRoot() {
		return remoteRoot;
	}
	public void setRemoteRoot(String remoteRoot) {
		this.remoteRoot = remoteRoot;
	}
}
