package co.codewizards.cloudstore.shared.repo.local;

import java.io.File;
import java.net.URL;

import co.codewizards.cloudstore.shared.dto.EntityID;
import co.codewizards.cloudstore.shared.persistence.LocalRepository;
import co.codewizards.cloudstore.shared.persistence.RemoteRepository;
import co.codewizards.cloudstore.shared.progress.ProgressMonitor;

public interface LocalRepoManager {

	public static final String META_DIR_NAME = ".cloudstore-repo";

	/**
	 * Gets the repository's local root directory.
	 * <p>
	 * This file is canonical (absolute and symbolic links resolved).
	 * @return the repository's local root directory. Never <code>null</code>.
	 */
	File getLocalRoot();

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
	 * Adds a remote repository to the local database.
	 * @param entityID the remote repository's unique ID. Must not be <code>null</code>. This is
	 * {@link LocalRepository#getEntityID() LocalRepository.entityID} in the remote database and will become
	 * {@link RemoteRepository#getEntityID() RemoteRepository.entityID} in the local database.
	 * @param remoteRoot the URL of the remote repository. Must not be <code>null</code>.
	 */
	void addRemoteRepository(EntityID entityID, URL remoteRoot);

	/**
	 * Moves the remote repository (in the local database) to another URL.
	 * @param entityID the remote repository's unique ID. Must not be <code>null</code>.
	 * @param newRemoteRoot the new URL of the remote repository. Must not be <code>null</code>.
	 */
	void moveRemoteRepository(EntityID entityID, URL newRemoteRoot);

	/**
	 * Deletes a remote repository from the local database.
	 * <p>
	 * Does nothing, if the specified repository does not exist.
	 * @param entityID the remote repository's unique ID. Must not be <code>null</code>.
	 */
	void deleteRemoteRepository(EntityID entityID);

}