package co.codewizards.cloudstore.rest.client.request;

import static java.util.Objects.*;

import javax.ws.rs.core.Response;

public class EndSyncFromRepository extends VoidRequest {

	private final String repositoryName;

	public EndSyncFromRepository(final String repositoryName) {
		this.repositoryName = requireNonNull(repositoryName, "repositoryName");
	}

	@Override
	public Response _execute() {
		return assignCredentials(
				createWebTarget("_endSyncFromRepository", urlEncode(repositoryName))
				.request()).post(null);
	}

}
