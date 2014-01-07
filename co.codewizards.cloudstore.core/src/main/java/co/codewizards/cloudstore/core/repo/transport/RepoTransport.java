package co.codewizards.cloudstore.core.repo.transport;

import java.io.File;
import java.net.URL;
import java.util.Date;

import co.codewizards.cloudstore.core.dto.ChangeSet;
import co.codewizards.cloudstore.core.dto.EntityID;
import co.codewizards.cloudstore.core.dto.FileChunkSet;
import co.codewizards.cloudstore.core.dto.RepositoryDTO;

public interface RepoTransport {

	RepoTransportFactory getRepoTransportFactory();
	void setRepoTransportFactory(RepoTransportFactory repoTransportFactory);

	URL getRemoteRoot();
	void setRemoteRoot(URL remoteRoot);

	RepositoryDTO getRepositoryDTO();

	/**
	 * Get the repository's unique ID.
	 * @return the repository's unique ID.
	 */
	EntityID getRepositoryID();

	void close();

	ChangeSet getChangeSet(EntityID toRepositoryID);

	/**
	 * Creates the specified directory (including all parent-directories).
	 * <p>
	 * If the directory already exists, this is a noop.
	 * <p>
	 * If there is any obstruction in the way of this path (e.g. a normal file), it is moved away (renamed or simply deleted
	 * depending on the conflict resolution strategy).
	 * @param path the path of the directory. Must not be <code>null</code>. No matter which operating system is used,
	 * the separation-character is always '/'. This path may start with a "/", but there is no difference, if it does:
	 * It is always relative to the repository's root directory.
	 * @param lastModified the {@link File#lastModified() File.lastModified} the newly created directory will be set to.
	 * May be <code>null</code> (in which case the {@code lastModified} property is not touched). This applies only to the
	 * actual directory and not to the parent-directories! The parent-directories' {@code lastModified} properties are never
	 * touched - even if the parent-directories are newly created.
	 */
	void makeDirectory(String path, Date lastModified);

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

	FileChunkSet getFileChunkSet(String path);

	/**
	 * Get the binary file data at the given {@code offset} and with the given {@code length}.
	 * <p>
	 * If the file was modified/deleted, this method should not fail, but simply return <code>null</code>
	 * or a result being shorter than the {@code length} specified.
	 * @param length the length of the data to be read.
	 * @param offset the offset of the first byte to be read (0-based).
	 */
	byte[] getFileData(String path, long offset, int length);

	/**
	 * Begins a file transfer to this {@code RepoTransport}.
	 * <p>
	 * Usually, this method creates the specified file in the file system (if necessary with parent-directories)
	 * and in the database. But this operation may be deferred until {@link #endFile(String, Date, long)}.
	 * <p>
	 * If the file is immediately created, it should not be synchronised to any other repository, yet! It should
	 * be ignored, until {@link #endFile(String, Date, long)} was called for it.
	 * <p>
	 * In normal operation, zero or more invocations of {@link #putFileData(String, long, byte[])} and
	 * finally one invocation of {@link #endFile(String, Date, long)} follow this method. However, this is not
	 * guaranteed and the file transfer may be interrupted. If it is resumed, later this method is called again,
	 * without {@link #endFile(String, Date, long)} ever having been called inbetween.
	 * @param path the path of the file. Must not be <code>null</code>. No matter which operating system is used,
	 * the separation-character is always '/'. This path may start with a "/", but there is no difference, if it does:
	 * It is always relative to the repository's root directory.
	 * @see #putFileData(String, long, byte[])
	 * @see #endFile(String, Date, long)
	 */
	void beginFile(String path);

	/**
	 * Write a block of binary data into the file.
	 * <p>
	 * This method may only be called after {@link #beginFile(String)} and before {@link #endFile(String, Date, long)}.
	 * @param offset the 0-based position in the file at which the block should be written.
	 * @see #beginFile(String)
	 * @see #endFile(String, Date, long)
	 */
	void putFileData(String path, long offset, byte[] fileData);

	void endFile(String path, Date lastModified, long length);

	void endSync(EntityID toRepositoryID);

}
