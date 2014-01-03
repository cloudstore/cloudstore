package co.codewizards.cloudstore.core.repo.local;

import java.io.File;
import java.net.URL;

import co.codewizards.cloudstore.core.dto.EntityID;
import co.codewizards.cloudstore.core.persistence.LocalRepository;
import co.codewizards.cloudstore.core.persistence.RemoteRepository;
import co.codewizards.cloudstore.core.progress.ProgressMonitor;

public interface LocalRepoManager {

	public static final String META_DIR_NAME = ".cloudstore-repo";

	/**
	 * Gets the repository's local root directory.
	 * <p>
	 * This file is canonical (absolute and symbolic links resolved).
	 * @return the repository's local root directory. Never <code>null</code>.
	 */
	File getLocalRoot();

	/**
	 * Gets the local repository's unique ID.
	 * <p>
	 * This is {@link LocalRepository#getEntityID() LocalRepository.entityID} in the local repository database.
	 * @return the local repository's unique ID. Never <code>null</code>.
	 */
	EntityID getLocalRepositoryID();

	void addLocalRepoManagerCloseListener(LocalRepoManagerCloseListener listener);

	void removeLocalRepoManagerCloseListener(LocalRepoManagerCloseListener listener);

	/**
	 * Gets the <i>open</i> state.
	 * <p>
	 * If this is <code>false</code>, the {@link LocalRepoManager} instance cannot be used anymore.
	 * Due to the proxy-mechanism, this does, however, not mean that the backend is really shut down.
	 * @return the <i>open</i> state.
	 */
	boolean isOpen();

	/**
	 * Closes this {@link LocalRepoManager}.
	 * <p>
	 * <b>Important:</b> The {@link LocalRepoManagerFactory} always returns a proxy. It never returns
	 * the real backend-instance. Calling {@code close()} closes the proxy and thus renders it unusable.
	 * It decrements the real backend-instance's reference counter. As soon as this reaches 0, the backend
	 * is really closed - which may happen delayed (for performance reasons).
	 */
	void close();

	LocalRepoTransaction beginTransaction();

	/**
	 * Synchronises the local file system with the local database.
	 * <p>
	 * Registers every directory and file in the repository's {@link #getLocalRoot() local root} and its
	 * sub-directories.
	 */
	void localSync(ProgressMonitor monitor);

	/**
	 * Adds or relocates a remote repository.
	 * @param repositoryID the remote repository's unique ID. Must not be <code>null</code>. This is
	 * {@link LocalRepository#getEntityID() LocalRepository.entityID} in the remote database and will become
	 * {@link RemoteRepository#getEntityID() RemoteRepository.entityID} in the local database.
	 * @param remoteRoot the URL of the remote repository. May be <code>null</code> (in the server, a
	 * {@code RemoteRepository} never has a {@code remoteRoot}).
	 */
	void putRemoteRepository(EntityID repositoryID, URL remoteRoot);

	/**
	 * Deletes a remote repository from the local database.
	 * <p>
	 * Does nothing, if the specified repository does not exist.
	 * @param repositoryID the remote repository's unique ID. Must not be <code>null</code>.
	 */
	void deleteRemoteRepository(EntityID repositoryID);

}