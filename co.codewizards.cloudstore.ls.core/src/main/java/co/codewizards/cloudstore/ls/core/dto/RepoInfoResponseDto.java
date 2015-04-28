package co.codewizards.cloudstore.ls.core.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class RepoInfoResponseDto {

	private UUID repositoryId;
	private String localRoot;
	private List<String> repositoryAliases;
	private byte[] publicKey;

	private List<RemoteRepositoryDto> remoteRepositoryDtos;
	private List<RemoteRepositoryRequestDto> remoteRepositoryRequestDtos;

	private long normalFileCount;
	private long directoryCount;
	private long copyModificationCount;
	private long deleteModificationCount;

	public UUID getRepositoryId() {
		return repositoryId;
	}
	public void setRepositoryId(final UUID repositoryId) {
		this.repositoryId = repositoryId;
	}
	public String getLocalRoot() {
		return localRoot;
	}
	public void setLocalRoot(final String localRoot) {
		this.localRoot = localRoot;
	}
	public List<String> getRepositoryAliases() {
		if (repositoryAliases == null)
			repositoryAliases = new ArrayList<>();

		return repositoryAliases;
	}
	public void setRepositoryAliases(final List<String> repositoryAliases) {
		this.repositoryAliases = repositoryAliases;
	}
	public byte[] getPublicKey() {
		return publicKey;
	}
	public void setPublicKey(final byte[] publicKey) {
		this.publicKey = publicKey;
	}

	public List<RemoteRepositoryDto> getRemoteRepositoryDtos() {
		if (remoteRepositoryDtos == null)
			remoteRepositoryDtos = new ArrayList<>();

		return remoteRepositoryDtos;
	}
	public void setRemoteRepositoryDtos(final List<RemoteRepositoryDto> remoteRepositoryDtos) {
		this.remoteRepositoryDtos = remoteRepositoryDtos;
	}

	public List<RemoteRepositoryRequestDto> getRemoteRepositoryRequestDtos() {
		if (remoteRepositoryRequestDtos == null)
			remoteRepositoryRequestDtos = new ArrayList<>();

		return remoteRepositoryRequestDtos;
	}
	public void setRemoteRepositoryRequestDtos(final List<RemoteRepositoryRequestDto> remoteRepositoryRequestDtos) {
		this.remoteRepositoryRequestDtos = remoteRepositoryRequestDtos;
	}

	public long getNormalFileCount() {
		return normalFileCount;
	}
	public void setNormalFileCount(long normalFileCount) {
		this.normalFileCount = normalFileCount;
	}
	public long getDirectoryCount() {
		return directoryCount;
	}
	public void setDirectoryCount(long directoryCount) {
		this.directoryCount = directoryCount;
	}
	public long getCopyModificationCount() {
		return copyModificationCount;
	}
	public void setCopyModificationCount(long copyModificationCount) {
		this.copyModificationCount = copyModificationCount;
	}
	public long getDeleteModificationCount() {
		return deleteModificationCount;
	}
	public void setDeleteModificationCount(long deleteModificationCount) {
		this.deleteModificationCount = deleteModificationCount;
	}
}
