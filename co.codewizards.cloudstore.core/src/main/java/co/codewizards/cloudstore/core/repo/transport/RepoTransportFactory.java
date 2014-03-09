package co.codewizards.cloudstore.core.repo.transport;

import java.net.URL;
import java.util.UUID;

/**
 * Factory creating instances of classes implementing {@link RepoTransport}.
 * <p>
 * <b>Important:</b> Implementors should <i>not</i> implement this interface directly. Instead,
 * {@link AbstractRepoTransportFactory} should be sub-classed.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public interface RepoTransportFactory {

	/**
	 * Gets the priority of this factory.
	 * <p>
	 * Factories are sorted primarily by this priority (descending). Thus, the greater the priority as a number
	 * the more likely it will be used.
	 * <p>
	 * Or in other words: The factory with the highest priority is chosen.
	 * <p>
	 * The default implementation in {@link AbstractRepoTransportFactory} returns 0. Thus, if you implement your
	 * own factory and register it for a URL type that is already handled by another factory,
	 * you must return a number greater than the other factory's priority (i.e. usually &gt 0).
	 * @return the priority of this factory.
	 */
	int getPriority();

	/**
	 * Gets the human-readable short name of this factory.
	 * <p>
	 * This should be a very short name like "File", "REST", "SOAP", etc. to be listed in a
	 * combo box or similar UI element.
	 * @return the human-readable short name of this factory. May be <code>null</code>, but
	 * implementors are highly discouraged to return <code>null</code> (or an empty string)!
	 * @see #getDescription()
	 */
	String getName();

	/**
	 * Gets the human-readable long description of this factory.
	 * <p>
	 * In contrast to {@link #getName()}, this method should provide an elaborate decription. It may be
	 * composed of multiple complete sentences and it may contain line breaks.
	 * @return the human-readable long description of this factory.  May be <code>null</code>. But
	 * implementors are encouraged to provide a meaningful description.
	 * @see #getName()
	 */
	String getDescription();

	/**
	 * Determine, whether the factory (or more precisely its {@link RepoTransport}s) is able to handle the given URL.
	 * @param remoteRoot the URL of the repository. Must not be <code>null</code>. This does not necessarily mean
	 * the repository is on a remote machine. It just means it is somewhere beyond this abstraction layer and might
	 * very well be on a remote server.
	 * @return <code>true</code>, if the URL is supported (i.e. a {@link RepoTransport} created by this factory will
	 * operate with it); <code>false</code>, if the URL is not supported.
	 */
	boolean isSupported(URL remoteRoot);

	/**
	 * Create a {@link RepoTransport} instance.
	 * @param remoteRoot the remote-root. Must not be <code>null</code>.
	 * @param clientRepositoryId the client-side repository's ID (i.e. the ID of the repo on the other side).
	 * May be <code>null</code>, if there is no repo (yet) on the other side. Note, that certain methods
	 * are not usable, if this is <code>null</code>.
	 * @return a new {@link RepoTransport} instance. Never <code>null</code>.
	 */
	RepoTransport createRepoTransport(URL remoteRoot, UUID clientRepositoryId);

}
