package co.codewizards.cloudstore.rest.client.request;

import static java.util.Objects.*;

import javax.ws.rs.core.Response;

public class BeginPutFile extends VoidRequest {

	protected final String repositoryName;
	protected final String path;

	public BeginPutFile(final String repositoryName, final String path) {
		this.repositoryName = requireNonNull(repositoryName, "repositoryName");
		this.path = path;
	}

	@Override
	public Response _execute() {
		return assignCredentials(
				createWebTarget("_beginPutFile", urlEncode(repositoryName), encodePath(path))
				.request()).put(null);
	}

}
