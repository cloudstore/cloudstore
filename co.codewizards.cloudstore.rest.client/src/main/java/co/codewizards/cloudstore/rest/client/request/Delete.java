package co.codewizards.cloudstore.rest.client.request;

import static java.util.Objects.*;

import javax.ws.rs.core.Response;

public class Delete extends VoidRequest {

	protected final String repositoryName;
	protected final String path;

	public Delete(final String repositoryName, final String path) {
		this.repositoryName = requireNonNull(repositoryName, "repositoryName");
		this.path = path;
	}

	@Override
	protected Response _execute() {
		return assignCredentials(
				createWebTarget(urlEncode(repositoryName), encodePath(path)).request()).delete();
	}

}
