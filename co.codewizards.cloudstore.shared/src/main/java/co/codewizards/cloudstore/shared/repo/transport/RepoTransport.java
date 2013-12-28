package co.codewizards.cloudstore.shared.repo.transport;

import java.net.URL;

public interface RepoTransport {

	RepoTransportFactory getRepoTransportFactory();
	void setRepoTransportFactory(RepoTransportFactory repoTransportFactory);

	URL getRemoteRoot();
	void setRemoteRoot(URL remoteRoot);



}
