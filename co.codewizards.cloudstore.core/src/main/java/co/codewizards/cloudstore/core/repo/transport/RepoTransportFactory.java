package co.codewizards.cloudstore.core.repo.transport;

import java.net.URL;

import co.codewizards.cloudstore.core.dto.EntityID;

public interface RepoTransportFactory {

	int getPriority();

	String getName();

	String getDescription();

	boolean isSupported(URL remoteRoot);

	/**
	 * Create a {@link RepoTransport} instance.
	 * @param remoteRoot the remote-root. Must not be <code>null</code>.
	 * @param clientRepositoryID the client-side repository's ID (i.e. the ID of the repo on the other side).
	 * May be <code>null</code>, if there is no repo (yet) on the other side. Note, that certain methods
	 * are not usable, if this is <code>null</code>.
	 * @return a new {@link RepoTransport} instance. Never <code>null</code>.
	 */
	RepoTransport createRepoTransport(URL remoteRoot, EntityID clientRepositoryID);

}
