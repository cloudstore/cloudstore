package co.codewizards.cloudstore.core.repo.transport;

import java.net.URL;

import co.codewizards.cloudstore.core.dto.ChangeSetRequest;
import co.codewizards.cloudstore.core.dto.ChangeSetResponse;

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
	 * the separation-character is always '/'.
	 * This path may start with a "/", but there is no difference, if it does: It is always relative to the repository's root directory.
	 */
	void makeDirectory(String path);

	/**
	 * @deprecated Only temporarily added!
	 */
	@Deprecated
	byte[] getFileData(String path); // TODO remove this method again!

	/**
	 * @deprecated Only temporarily added!
	 */
	@Deprecated
	void putFileData(String path, byte[] fileData); // TODO remove this method again!

}
