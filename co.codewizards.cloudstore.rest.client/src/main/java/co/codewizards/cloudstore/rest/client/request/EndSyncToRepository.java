package co.codewizards.cloudstore.rest.client.request;

import static java.util.Objects.*;

import javax.ws.rs.core.Response;

public class EndSyncToRepository extends VoidRequest {

	private final String repositoryName;
	private final long fromLocalRevision;

	public EndSyncToRepository(final String repositoryName, final long fromLocalRevision) {
		this.repositoryName = requireNonNull(repositoryName, "repositoryName");
		this.fromLocalRevision = fromLocalRevision;
	}

	@Override
	public Response _execute() {
		return assignCredentials(
				createWebTarget("_endSyncToRepository", urlEncode(repositoryName))
				.queryParam("fromLocalRevision", fromLocalRevision)
				.request()).post(null);
	}

}
