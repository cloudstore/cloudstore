package co.codewizards.cloudstore.core.repo.transport;

import java.net.URL;
import java.util.Date;

import co.codewizards.cloudstore.core.dto.ChangeSetRequest;
import co.codewizards.cloudstore.core.dto.ChangeSetResponse;
import co.codewizards.cloudstore.core.dto.FileChunkSetRequest;
import co.codewizards.cloudstore.core.dto.FileChunkSetResponse;

public interface RepoTransport {

	RepoTransportFactory getRepoTransportFactory();
	void setRepoTransportFactory(RepoTransportFactory repoTransportFactory);

	URL getRemoteRoot();
	void setRemoteRoot(URL remoteRoot);

	void close();

	ChangeSetResponse getChangeSet(ChangeSetRequest changeSetRequest);

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
	 */
	void makeDirectory(String path);

	/**
	 * Creates the specified file in the file system (if necessary with parent-directories) and the database.
	 * <p>
	 * The file should not be synchronised to any other repository, yet! It should be ignored, until
	 * {@link #setLastModified(String, Date)} was called for it.
	 * @param path the path of the file. Must not be <code>null</code>. No matter which operating system is used,
	 * the separation-character is always '/'. This path may start with a "/", but there is no difference, if it does:
	 * It is always relative to the repository's root directory.
	 */
	void createFile(String path);

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

	FileChunkSetResponse getFileChunkSet(FileChunkSetRequest fileChunkSetRequest);

	void setLastModified(String path, Date lastModified);

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
	 * @param offset
	 */
	void putFileData(String path, long offset, byte[] fileData);

}
