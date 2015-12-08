package co.codewizards.cloudstore.core.repo.local;

import java.lang.reflect.InvocationHandler;
import java.net.URL;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.Lock;

import co.codewizards.cloudstore.core.appid.AppIdRegistry;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.progress.ProgressMonitor;

public interface LocalRepoManager extends AutoCloseable {
	String APP_ID_SIMPLE_ID = AppIdRegistry.getInstance().getAppIdOrFail().getSimpleId();

	String SYSTEM_PROPERTY_KEY_SIZE = APP_ID_SIMPLE_ID + ".repository.asymmetricKey.size";
	int DEFAULT_KEY_SIZE = 4096;

	String SYSTEM_PROPERTY_CLOSE_DEFERRED_MILLIS = APP_ID_SIMPLE_ID + ".localRepoManager.closeDeferredMillis";
	long DEFAULT_CLOSE_DEFERRED_MILLIS = 20000;

	String META_DIR_NAME = "." + APP_ID_SIMPLE_ID + "-repo";
	String TEMP_DIR_NAME = "." + APP_ID_SIMPLE_ID + "-tmp";
	String TEMP_NEW_FILE_PREFIX = "." + APP_ID_SIMPLE_ID + "-new_";

	String REPOSITORY_PROPERTIES_FILE_NAME = APP_ID_SIMPLE_ID + "-repository.properties";
	String PROP_REPOSITORY_ID = "repository.id";
	String PROP_VERSION = "repository.version";
	/**
	 * Aliases separated by '/' (because '/' is an illegal character for an alias).
	 * <p>
	 * To make scripting easier (e.g. using grep), the aliases start and end with a
	 * '/'. For example: "/alias1/alias2/alias3/"
	 */
	String PROP_REPOSITORY_ALIASES = "repository.aliases";

	String PERSISTENCE_PROPERTIES_FILE_NAME = APP_ID_SIMPLE_ID + "-persistence.properties";

	String VAR_REPOSITORY_ID = "repository.id";
	String VAR_LOCAL_ROOT = "repository.localRoot";
	String VAR_META_DIR = "repository.metaDir";

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
	UUID getRepositoryId();

	/**
	 * Gets the local repository's private key.
	 * <p>
	 * This is always an RSA key - other key types are not (yet) supported.
	 * @return the local repository's private key. Never <code>null</code>.
	 */
	byte[] getPrivateKey();

	/**
	 * Gets the local repository's public key.
	 * <p>
	 * This is always an RSA key - other key types are not (yet) supported.
	 * @return the local repository's public key. Never <code>null</code>.
	 */
	byte[] getPublicKey();

	/**
	 * Gets the remote repository's public key.
	 * <p>
	 * This is always an RSA key - other key types are not (yet) supported.
	 * @param repositoryId the remote repository's unique ID. Must not be <code>null</code>.
	 * @return the remote repository's public key. Never <code>null</code>.
	 * @throws IllegalArgumentException if there is no remote-repository with the given {@code repositoryId}.
	 */
	byte[] getRemoteRepositoryPublicKeyOrFail(UUID repositoryId);

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
	@Override
	void close();

	/**
	 * Begin a JDO transaction for read operations only in the underlying database.
	 * @return the transaction handle. Never <code>null</code>.
	 */
	LocalRepoTransaction beginReadTransaction();

	/**
	 * Begin a JDO transaction for read and write operations in the underlying database.
	 * @return the transaction handle. Never <code>null</code>.
	 */
	LocalRepoTransaction beginWriteTransaction();

	/**
	 * Synchronises the local file system with the local database.
	 * <p>
	 * Registers every directory and file in the repository's {@link #getLocalRoot() local root} and its
	 * sub-directories.
	 */
	void localSync(ProgressMonitor monitor);

	/**
	 * Adds or relocates a remote repository.
	 * @param repositoryId the remote repository's unique ID. Must not be <code>null</code>. This is
	 * {@link LocalRepository#getEntityID() LocalRepository.entityID} in the remote database and will become
	 * {@link RemoteRepository#getEntityID() RemoteRepository.entityID} in the local database.
	 * @param remoteRoot the URL of the remote repository. May be <code>null</code> (in the server, a
	 * {@code RemoteRepository} never has a {@code remoteRoot}).
	 * @param localPathPrefix TODO
	 */
	void putRemoteRepository(UUID repositoryId, URL remoteRoot, byte[] publicKey, String localPathPrefix);

	/**
	 * Deletes a remote repository from the local database.
	 * <p>
	 * Does nothing, if the specified repository does not exist.
	 * @param repositoryId the remote repository's unique ID. Must not be <code>null</code>.
	 */
	void deleteRemoteRepository(UUID repositoryId);

	Map<UUID, URL> getRemoteRepositoryId2RemoteRootMap();

	/**
	 * Gets the local path-prefix (of the local repository managed by this {@code LocalRepoManager}) when syncing with
	 * the remote repository identified by the given {@code remoteRoot}.
	 * @param remoteRoot the remote repository's root-URL (not necessarily its real root, but the root URL connected
	 * to the local repository). Must not be <code>null</code>.
	 * @return the local path-prefix. Never <code>null</code>, but maybe empty.
	 * @throws IllegalArgumentException if there is no remote-repository with the given {@code remoteRoot}.
	 */
	String getLocalPathPrefixOrFail(URL remoteRoot);

	/**
	 * Gets the local path-prefix (of the local repository managed by this {@code LocalRepoManager}) when syncing with
	 * the remote repository identified by the given {@code remoteRoot}.
	 * @param repositoryId the remote repository's unique ID. Must not be <code>null</code>.
	 * @return the local path-prefix. Never <code>null</code>, but maybe empty.
	 * @throws IllegalArgumentException if there is no remote-repository with the given {@code remoteRoot}.
	 */
	String getLocalPathPrefixOrFail(UUID repositoryId);

	/**
	 * Gets the unique ID of the remote repository identified by the given {@code remoteRoot}.
	 * @param remoteRoot the remote repository's root-URL (not necessarily its real root, but the root URL connected
	 * to the local repository). Must not be <code>null</code>.
	 * @return the remote repository's unique ID. Never <code>null</code>.
	 * @throws IllegalArgumentException if there is no remote-repository with the given {@code remoteRoot}.
	 */
	UUID getRemoteRepositoryIdOrFail(URL remoteRoot);

	Lock getLock();

	/**
	 * @deprecated <b>Do not invoke this method directly!</b> It is declared in this interface to make sure the
	 * proxy's {@link InvocationHandler} is invoked when the garbage-collector collects the proxy.
	 */
	@Deprecated
	void finalize() throws Throwable;

	void putRepositoryAlias(String repositoryAlias);

	void removeRepositoryAlias(String repositoryAlias);

	LocalRepoMetaData getLocalRepoMetaData();
}