package co.codewizards.cloudstore.shared.repo.transport;

import java.net.URL;

import co.codewizards.cloudstore.shared.dto.ChangeSetRequest;
import co.codewizards.cloudstore.shared.dto.ChangeSetResponse;
import co.codewizards.cloudstore.shared.dto.StringList;

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
	 * If the directory already exists and {@code childNamesToKeep} is <code>null</code>, this is a noop.
	 * <p>
	 * If there is any obstruction in the way of this path (e.g. a normal file), it is moved away (renamed or simply deleted
	 * depending on the conflict resolution strategy).
	 * <p>
	 * If {@code childNamesToKeep} is <i>not</i> <code>null</code> and the directory already exists, all children
	 * which are not contained in {@code childNamesToKeep} are deleted.
	 * @param path the path of the directory. Must not be <code>null</code>. No matter which operating system is used,
	 * the separation-character is always '/'.
	 * This path may start with a "/", but there is no difference, if it does: It is always relative to the repository's root directory.
	 * @param childNamesToKeep an optional list of children to the specified directory. If not <code>null</code>, all
	 * children which are not enlisted here, are deleted.
	 */
	void makeDirectory(String path, StringList childNamesToKeep);

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
