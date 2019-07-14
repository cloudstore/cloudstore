package co.codewizards.cloudstore.core.repo.sync;

import static java.util.Objects.*;

import java.net.URL;
import java.util.Date;
import java.util.UUID;

import co.codewizards.cloudstore.core.dto.ChangeSetDto;
import co.codewizards.cloudstore.core.dto.ConfigPropSetDto;
import co.codewizards.cloudstore.core.dto.RepoFileDto;
import co.codewizards.cloudstore.core.dto.RepositoryDto;
import co.codewizards.cloudstore.core.dto.VersionInfoDto;
import co.codewizards.cloudstore.core.repo.transport.RepoTransport;
import co.codewizards.cloudstore.core.repo.transport.RepoTransportFactory;

public class RepoTransportRef implements RepoTransport {

	private RepoTransport delegate;

	public RepoTransport getDelegate() {
		return delegate;
	}

	public void setDelegate(RepoTransport delegate) {
		this.delegate = delegate;
	}

	public RepoTransport getDelegateOrFail() {
		return requireNonNull(getDelegate(), "delegate");
	}

	@Override
	public RepoTransportFactory getRepoTransportFactory() {
		return getDelegateOrFail().getRepoTransportFactory();
	}

	@Override
	public void setRepoTransportFactory(RepoTransportFactory repoTransportFactory) {
		getDelegateOrFail().setRepoTransportFactory(repoTransportFactory);
	}

	@Override
	public URL getRemoteRoot() {
		return getDelegateOrFail().getRemoteRoot();
	}

	@Override
	public void setRemoteRoot(URL remoteRoot) {
		getDelegateOrFail().setRemoteRoot(remoteRoot);
	}

	@Override
	public UUID getClientRepositoryId() {
		return getDelegateOrFail().getClientRepositoryId();
	}

	@Override
	public void setClientRepositoryId(UUID clientRepositoryId) {
		getDelegateOrFail().setClientRepositoryId(clientRepositoryId);
	}

	@Override
	public URL getRemoteRootWithoutPathPrefix() {
		return getDelegateOrFail().getRemoteRootWithoutPathPrefix();
	}

	@Override
	public String getPathPrefix() {
		return getDelegateOrFail().getPathPrefix();
	}

	@Override
	public String prefixPath(String path) {
		return getDelegateOrFail().prefixPath(path);
	}

	@Override
	public String unprefixPath(String path) {
		return getDelegateOrFail().unprefixPath(path);
	}

	@Override
	public RepositoryDto getRepositoryDto() {
		return getDelegateOrFail().getRepositoryDto();
	}

	@Override
	public UUID getRepositoryId() {
		return getDelegateOrFail().getRepositoryId();
	}

	@Override
	public byte[] getPublicKey() {
		return getDelegateOrFail().getPublicKey();
	}

	@Override
	public void requestRepoConnection(byte[] publicKey) {
		getDelegateOrFail().requestRepoConnection(publicKey);
	}

	@Override
	public ChangeSetDto getChangeSetDto(boolean localSync, Long lastSyncToRemoteRepoLocalRepositoryRevisionSynced) {
		return getDelegateOrFail().getChangeSetDto(localSync, lastSyncToRemoteRepoLocalRepositoryRevisionSynced);
	}

	@Override
	public void prepareForChangeSetDto(ChangeSetDto changeSetDto) {
		getDelegateOrFail().prepareForChangeSetDto(changeSetDto);
	}

	@Override
	public void makeDirectory(String path, Date lastModified) {
		getDelegateOrFail().makeDirectory(path, lastModified);
	}

	@Override
	public void makeSymlink(String path, String target, Date lastModified) {
		getDelegateOrFail().makeSymlink(path, target, lastModified);
	}

	@Override
	public void copy(String fromPath, String toPath) {
		getDelegateOrFail().copy(fromPath, toPath);
	}

	@Override
	public void move(String fromPath, String toPath) {
		getDelegateOrFail().move(fromPath, toPath);
	}

	@Override
	public void delete(String path) {
		getDelegateOrFail().delete(path);
	}

	@Override
	public RepoFileDto getRepoFileDto(String path) {
		return getDelegateOrFail().getRepoFileDto(path);
	}

	@Override
	public byte[] getFileData(String path, long offset, int length) {
		return getDelegateOrFail().getFileData(path, offset, length);
	}

	@Override
	public void beginPutFile(String path) {
		getDelegateOrFail().beginPutFile(path);
	}

	@Override
	public void putFileData(String path, long offset, byte[] fileData) {
		getDelegateOrFail().putFileData(path, offset, fileData);
	}

	@Override
	public void endPutFile(String path, Date lastModified, long length, String sha1) {
		getDelegateOrFail().endPutFile(path, lastModified, length, sha1);
	}

	@Override
	public void endSyncFromRepository() {
		getDelegateOrFail().endSyncFromRepository();
	}

	@Override
	public void endSyncToRepository(long fromLocalRevision) {
		getDelegateOrFail().endSyncToRepository(fromLocalRevision);
	}

	@Override
	public void close() {
		RepoTransport d = getDelegate();
		if (d != null)
			d.close();
	}

	@Override
	public void putParentConfigPropSetDto(ConfigPropSetDto parentConfigPropSetDto) {
		getDelegateOrFail().putParentConfigPropSetDto(parentConfigPropSetDto);
	}

	@Override
	public VersionInfoDto getVersionInfoDto() {
		return getDelegateOrFail().getVersionInfoDto();
	}

	@Override
	public RepositoryDto getClientRepositoryDto() {
		return getDelegateOrFail().getClientRepositoryDto();
	}
}
