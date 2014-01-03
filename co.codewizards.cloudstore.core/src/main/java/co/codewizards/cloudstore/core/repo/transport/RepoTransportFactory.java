package co.codewizards.cloudstore.core.repo.transport;

import java.net.URL;

public interface RepoTransportFactory {

	int getPriority();

	String getName();

	String getDescription();

	boolean isSupported(URL remoteRoot);

	RepoTransport createRepoTransport(URL remoteRoot);

}
