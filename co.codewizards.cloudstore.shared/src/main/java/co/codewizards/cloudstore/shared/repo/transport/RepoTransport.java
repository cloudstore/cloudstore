package co.codewizards.cloudstore.shared.repo.transport;

import java.net.URL;

import co.codewizards.cloudstore.shared.dto.ChangeSetRequest;
import co.codewizards.cloudstore.shared.dto.ChangeSetResponse;

public interface RepoTransport {

	RepoTransportFactory getRepoTransportFactory();
	void setRepoTransportFactory(RepoTransportFactory repoTransportFactory);

	URL getRemoteRoot();
	void setRemoteRoot(URL remoteRoot);

	ChangeSetResponse getChangeSet(ChangeSetRequest changeSetRequest);

}
