package co.codewizards.cloudstore.shared.repo.transport;

import java.net.URL;

import co.codewizards.cloudstore.shared.dto.RepoFileDTOList;

public interface RepoTransport {

	RepoTransportFactory getRepoTransportFactory();
	void setRepoTransportFactory(RepoTransportFactory repoTransportFactory);

	URL getRemoteRoot();
	void setRemoteRoot(URL remoteRoot);

	RepoFileDTOList getRepoFileDTOsChangedAfter(long localRevision);

}
