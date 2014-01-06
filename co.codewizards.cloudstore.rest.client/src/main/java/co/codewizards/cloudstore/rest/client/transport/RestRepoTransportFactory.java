package co.codewizards.cloudstore.rest.client.transport;

import java.net.URL;

import co.codewizards.cloudstore.core.repo.transport.AbstractRepoTransportFactory;
import co.codewizards.cloudstore.core.repo.transport.RepoTransport;

public class RestRepoTransportFactory extends AbstractRepoTransportFactory {

	public static final String PROTOCOL_HTTPS = "https";
	public static final String PROTOCOL_HTTP = "http";

	@Override
	public String getName() {
		return "REST";
	}

	@Override
	public String getDescription() {
		return "Repository on a remote server accessible via REST";
	}

	@Override
	public boolean isSupported(URL remoteRoot) {
		return PROTOCOL_HTTP.equals(remoteRoot.getProtocol()) || PROTOCOL_HTTPS.equals(remoteRoot.getProtocol());
	}

	@Override
	protected RepoTransport _createRepoTransport(URL remoteRoot) {
		return new RestRepoTransport();
	}
}
