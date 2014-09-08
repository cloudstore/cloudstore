package co.codewizards.cloudstore.core.repo.transport;

import java.net.URL;
import java.util.Date;
import java.util.UUID;

import co.codewizards.cloudstore.core.dto.ChangeSetDto;
import co.codewizards.cloudstore.core.dto.DirectoryDto;
import co.codewizards.cloudstore.core.dto.NormalFileDto;
import co.codewizards.cloudstore.core.dto.RepoFileDto;
import co.codewizards.cloudstore.core.dto.RepositoryDto;
import co.codewizards.cloudstore.core.dto.SymlinkDto;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.util.IOUtil;

/**
 * Transport abstraction.
 * <p>
 * The naming in this interface assumes a local client talking to a remote repository. But the
 * repository accessed via this transport does not need to be remote - it might be in the local
 * file system!
 * <p>
 * More precisely:
 * <p>
 * "Remote repository" references the repository which is accessed via this transport layer. The word
 * "remote" thus indicates that there <b>might</b> be some distance between here and wherever this repository
 * is located.
 * <p>
 * "Client" should primarily be understood as <i>API client</i>, i.e. the code using the methods of this
 * interface. The "client repository" is the repository for which some client code accesses this
 * {@code RepoTransport}, therefore the "client repository" is used for repo-to-repo-authentication. Some
 * methods in this interface can be used without authentication (i.e. anonymously) - therefore a "client
 * repository" is optional.
 * <p>
 * The synchronisation logic accesses all repositories through this abstraction layer. Therefore,
 * the synchronisation logic does not need to know any details about how to communicate with
 * a repository.
 * <p>
 * There are currently two implementations:
 * <ul>
 * <li>file-system-based (for local repositories)
 * <li>REST-based (for remote repositories)
 * </ul>
 * Further implementations might be written later.
 * <p>
 * An instance of an implementation of {@code RepoTransport} is obtained via the
 * {@link RepoTransportFactory}.
 * <p>
 * <b>Important:</b> Implementors should <i>not</i> directly implement this interface, but instead sub-class
 * {@link AbstractRepoTransport}!
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public interface RepoTransport extends AutoCloseable {

	/**
	 * Gets the factory which created this instance.
	 * @return the factory which created this instance. Should never be <code>null</code>, if properly initialised.
	 * @see #setRepoTransportFactory(RepoTransportFactory)
	 */
	RepoTransportFactory getRepoTransportFactory();
	/**
	 * Sets the factory which created this instance.
	 * @param repoTransportFactory the factory which created this instance. Must not be <code>null</code>.
	 * @see #getRepoTransportFactory()
	 */
	void setRepoTransportFactory(RepoTransportFactory repoTransportFactory);

	/**
	 * Gets the remote repository's root URL, maybe including a {@linkplain #getPathPrefix() path-prefix}.
	 * <p>
	 * This is thus the remote repository's root URL as used to synchronise a certain local repository.
	 * The word "remote" should not be misunderstood as actually on another computer. It just means behind
	 * this transport abstraction.
	 * <p>
	 * In contrast to the {@link #getRemoteRootWithoutPathPrefix() remoteRootWithoutPathPrefix}, this is
	 * the connection point for the synchronisation, which might be a sub-directory, i.e. not the native
	 * root of the connected repository.
	 * @return the remote repository's root URL, maybe including a {@linkplain #getPathPrefix() path-prefix}.
	 * Never <code>null</code>, if properly initialised.
	 * @see #setRemoteRoot(URL)
	 */
	URL getRemoteRoot();
	/**
	 * Sets the remote repository's root URL.
	 * <p>
	 * This URL is the point where the {@linkplain #getClientRepositoryId() client-repository} is connected
	 * to the repository managed by this {@code RepoTransport}.
	 * <p>
	 * You should never directly invoke this method! It is automatically called when creating a
	 * {@code RepoTransport} instance via the {@link RepoTransportFactory}.
	 * <p>
	 * Invoking this method twice with different {@code remoteRoot} values is not allowed. The {@code remoteRoot}
	 * cannot be changed after it was once set.
	 * @param remoteRoot the remote repository's root URL. It may be <code>null</code>, but this
	 * {@code RepoTransport} is only usable, after this method was invoked with a non-<code>null</code> value.
	 * @see #getRemoteRoot()
	 */
	void setRemoteRoot(URL remoteRoot);

	/**
	 * Gets the client repository's unique identifier.
	 * <p>
	 * The word "client" does not necessarily mean a remote JVM. It merely means the API client accessing
	 * this {@code RepoTransport} API.
	 * <p>
	 * This property might be <code>null</code>. If it is not set, only operations which do not require
	 * repo-to-repo-authentication can be used.
	 * @return the client repository's identifier. Might be <code>null</code>.
	 * @see #setClientRepositoryId(UUID)
	 */
	UUID getClientRepositoryId();
	/**
	 * Sets the client's repository identifier.
	 * @param clientRepositoryId the client's repository identifier. May be <code>null</code>.
	 * @see #getClientRepositoryId()
	 */
	void setClientRepositoryId(UUID clientRepositoryId);

	/**
	 * Gets the remote repository's root URL without the {@linkplain #getPathPrefix() path-prefix}.
	 * <p>
	 * In other words, this is the repository's <b>native root</b>, even if the connection is established to a
	 * sub-directory.
	 * @return the remote repository's root URL without the {@linkplain #getPathPrefix() path-prefix}. Never
	 * <code>null</code>, if properly initialised.
	 */
	URL getRemoteRootWithoutPathPrefix();

	/**
	 * Prefix for every path (as used in {@link #delete(String)} for example).
	 * <p>
	 * It is possible to connect to a repository at a sub-directory, i.e. not the repo's root. If this
	 * {@code RepoTransport} is connected to the repo's root, this {@code pathPrefix} is an empty string.
	 * But if this {@code RepoTransport} is connected to a sub-directory, this sub-directory will be the
	 * {@code pathPrefix}.
	 * <p>
	 * For example, if the {@link #getRemoteRoot() remoteRoot} is
	 * <code>"https://some.host/some/repo/Private+pictures/Wedding+%26+honeymoon"</code> and the
	 * {@link #getRemoteRootWithoutPathPrefix() remoteRootWithoutPathPrefix} is
	 * <code>"https://some.host/some/repo"</code>,
	 * then this {@code pathPrefix} will be <code>"/Private pictures/Wedding &amp; honeymoon"</code>.
	 * <p>
	 * As shown in this example, the {@code pathPrefix} is - just like every other path - <b>not</b> encoded
	 * in any way! The separator for the path-segments inside this prefix is "/" on all operating systems.
	 * <p>
	 * The {@code RepoTransport} implementations use this prefix to calculate back and forth between the
	 * path relative to the connected {@code remoteRoot} and the complete path used in the repository.
	 */
	String getPathPrefix();

	/**
	 * Prepend the {@link #getPathPrefix() pathPrefix} to the given {@code path}.
	 * @param path the path to be prepended. Must not be <code>null</code>.
	 * @return the complete path composed of the {@link #getPathPrefix() pathPrefix} and the given
	 * {@code path}. Never <code>null</code>.
	 * @see #unprefixPath(String)
	 * @see #getPathPrefix()
	 */
	String prefixPath(String path);

	/**
	 * Cut the {@link #getPathPrefix() pathPrefix} from the given {@code path}.
	 * @param path the path to be shortened. Must not be <code>null</code>. Of course, this path
	 * must start with {@link #getPathPrefix() pathPrefix}.
	 * @return the new shortened path without the {@link #getPathPrefix() pathPrefix}. Never
	 * <code>null</code>.
	 * @see #prefixPath(String)
	 * @see #getPathPrefix()
	 */
	String unprefixPath(String path);

	/**
	 * Gets the remote repository's repository-descriptor.
	 * <p>
	 * This operation does not require authentication! It can (and is regularly) invoked anonymously.
	 * @return the remote repository's repository-descriptor. Never <code>null</code>.
	 */
	RepositoryDto getRepositoryDto();

	/**
	 * Get the remote repository's unique identifier.
	 * @return the repository's unique identifier.
	 */
	UUID getRepositoryId();

	/**
	 * Gets the remote repository's public key.
	 * @return the remote repository's public key. Never <code>null</code>.
	 */
	byte[] getPublicKey();

	/**
	 * Request to connect the {@linkplain #getClientRepositoryId() client repository} with
	 * {@linkplain #getRepositoryId() the remote repository}.
	 * @param publicKey the public key of the client repository which requests the connection. Must not be
	 * <code>null</code>.
	 */
	void requestRepoConnection(byte[] publicKey);

	/**
	 * Gets the change-set from the remote repository.
	 * <p>
	 * The invocation of this method marks the beginning of a synchronisation. After the synchronisation is
	 * complete, the {@link #endSyncFromRepository()} method must be invoked to notify the remote repository
	 * that all changes contained in the change set have been successfully and completely written to the
	 * client repository.
	 * <p>
	 * The change-set is dependent on the client repository: Every client repository gets its own individual
	 * change-set. The remote repository tracks which changes need to be sent to the client. In normal
	 * operation, the same change is transferred only once. Under certain circumstances, however, the same
	 * change might be transferred multiple times and the client must cope with this! Such duplicate
	 * transfers happen, if the transfer is interrupted - i.e. the {@link #endSyncFromRepository()} was not
	 * invoked.
	 * <p>
	 * Please note that the DTOs in this {@link ChangeSetDto} do not need to be completely resolved. They
	 * might be incomplete in order to reduce the size of the {@link ChangeSetDto}. For example,
	 * {@link NormalFileDto#getFileChunkDtos() NormalFileDto.fileChunkDtos} is not populated. These details
	 * are separately requested, later - e.g. by {@link #getRepoFileDto(String)}.
	 * @param localSync <code>true</code> indicates that the remote repository should perform a local sync
	 * before calculating the change set. <code>false</code> indicates that the remote repository should
	 * abstain from a local sync. This flag is a hint and the remote repository does not need to adhere it.
	 * @return the change-set from the remote repository. Never <code>null</code>.
	 */
	ChangeSetDto getChangeSetDto(boolean localSync);

	/**
	 * Creates the specified directory (including all parent-directories as needed).
	 * <p>
	 * If the directory already exists, this is a noop.
	 * <p>
	 * If there is any obstruction in the way of this path (e.g. a normal file), it is moved away (renamed or simply deleted
	 * depending on the conflict resolution strategy).
	 * @param path the path of the directory. Must not be <code>null</code>. No matter which operating system is used,
	 * the separation-character is always '/'. This path may start with a "/", but there is no difference, if it does:
	 * It is always relative to the repository's root directory.
	 * @param lastModified the {@linkplain IOUtil#getLastModifiedNoFollow(File) last-modified-timestamp} the newly created
	 * directory will be set to.
	 * May be <code>null</code> (in which case the {@code lastModified} property is not touched). This applies only to the
	 * actual directory and not to the parent-directories! The parent-directories' {@code lastModified} properties are never
	 * touched - even if the parent-directories are newly created.
	 */
	void makeDirectory(String path, Date lastModified);

	void makeSymlink(String path, String target, Date lastModified);

	void copy(String fromPath, String toPath);
	void move(String fromPath, String toPath);

	/**
	 * Deletes the file (or directory) specified by {@code path}.
	 * <p>
	 * If there is no such file (or directory), this method is a noop.
	 * <p>
	 * If {@code path} denotes a directory, all its children (if there are) are deleted recursively.
	 * @param path the path of the file (or directory) to be deleted. Must not be <code>null</code>. No matter which
	 * operating system is used, the separation-character is always '/'. This path may start with a "/", but there is no
	 * difference, if it does: It is always relative to the repository's root directory.
	 */
	void delete(String path);

	/**
	 * Gets the data of the {@linkplain NormalFileDto file} (or {@linkplain DirectoryDto directory} or
	 * {@linkplain SymlinkDto symlink}) identified by the given {@code path}.
	 * @param path the path to the file.
	 * @return the data of the file referenced by {@code path}. Never <code>null</code>.
	 */
	RepoFileDto getRepoFileDto(String path);

	/**
	 * Get the binary file data at the given {@code offset} and with the given {@code length}.
	 * <p>
	 * If the file was modified/deleted, this method should not fail, but simply return <code>null</code>
	 * or a result being shorter than the {@code length} specified.
	 * @param path the path of the file. Must not be <code>null</code>. No matter which operating system is used,
	 * the separation-character is always '/'. This path may start with a "/", but there is no difference, if it does:
	 * It is always relative to the repository's root directory.
	 * @param offset the offset of the first byte to be read (0-based).
	 * @param length the length of the data to be read. -1 to read from {@code offset} to the end of the file.
	 */
	byte[] getFileData(String path, long offset, int length);

	/**
	 * Begins a file transfer to this {@code RepoTransport} (more precisely the remote repository behind it).
	 * <p>
	 * Usually, this method creates the specified file in the file system (if necessary with parent-directories)
	 * and in the database. But this operation may be deferred until {@link #endPutFile(String, Date, long, String)}.
	 * <p>
	 * If the file is immediately created, it should not be synchronised to any other repository, yet! It should
	 * be ignored, until {@link #endPutFile(String, Date, long, String)} was called for it.
	 * <p>
	 * In normal operation, zero or more invocations of {@link #putFileData(String, long, byte[])} and
	 * finally one invocation of {@link #endPutFile(String, Date, long, String)} follow this method. However, this is not
	 * guaranteed and the file transfer may be interrupted. If it is resumed, later this method is called again,
	 * without {@link #endPutFile(String, Date, long, String)} ever having been called inbetween.
	 * @param path the path of the file. Must not be <code>null</code>. No matter which operating system is used,
	 * the separation-character is always '/'. This path may start with a "/", but there is no difference, if it does:
	 * It is always relative to the repository's root directory.
	 * @see #putFileData(String, long, byte[])
	 * @see #endPutFile(String, Date, long, String)
	 */
	void beginPutFile(String path);

	/**
	 * Write a block of binary data into the file.
	 * <p>
	 * This method may only be called after {@link #beginPutFile(String)} and before {@link #endPutFile(String, Date, long, String)}.
	 * @param offset the 0-based position in the file at which the block should be written.
	 * @see #beginPutFile(String)
	 * @see #endPutFile(String, Date, long, String)
	 */
	void putFileData(String path, long offset, byte[] fileData);

	/**
	 * Ends a file transfer to this {@code RepoTransport} (more precisely the remote repository behind it).
	 * @param path the path of the file. Must not be <code>null</code>. No matter which operating system is used,
	 * the separation-character is always '/'. This path may start with a "/", but there is no difference, if it does:
	 * It is always relative to the repository's root directory.
	 * @param lastModified when was the file's last modification. Must not be <code>null</code>.
	 * @param length the length of the file in bytes. If the file already existed and was longer, it is
	 * truncated to this length.
	 * @param sha1 the SHA1 hash of the file. May be <code>null</code>. If it is given, the repository may
	 * log a warning, if the current file is different. It should not throw an exception, because it
	 * is a valid state that a file is modified (by another process) while it is transferred.
	 */
	void endPutFile(String path, Date lastModified, long length, String sha1);

	/**
	 * Marks the end of a synchronisation <b>from</b> the remote repository behind this {@code RepoTransport}.
	 * <p>
	 * This method should be invoked after all changes indicated by {@link #getChangeSetDto(boolean)} have
	 * been completely written into the client repository.
	 * <p>
	 * After this method was invoked, {@link #getChangeSetDto(boolean)} will return the new changes only.
	 * New changes means all those changes that were accumulated after its last invocation - not after the
	 * invocation of this method! This method might be called some time after {@code getChangeSetDto(...)}
	 * and it must be guaranteed that changes done between {@code getChangeSetDto(...)} and
	 * {@code endSyncFromRepository()} are contained in the next invocation of {@code getChangeSetDto(...)}.
	 * <p>
	 * This method must not be invoked, if an error was encountered during the synchronisation! It must thus
	 * not be used in a finally block! More invocations of {@code getChangeSetDto(...)} than of
	 * {@code endSyncFromRepository()} are totally fine.
	 */
	void endSyncFromRepository();

	/**
	 * Marks the end of a synchronisation <b>to</b> the remote repository behind this {@code RepoTransport}.
	 * <p>
	 * This method should be invoked after all changes in the client repository have been completely written
	 * into the remote repository behind this {@code RepoTransport}.
	 * @param fromLocalRevision the {@code localRevision} of the source-repository to which the destination
	 * repository is now synchronous.
	 */
	void endSyncToRepository(long fromLocalRevision);

	@Override
	public void close();

}
