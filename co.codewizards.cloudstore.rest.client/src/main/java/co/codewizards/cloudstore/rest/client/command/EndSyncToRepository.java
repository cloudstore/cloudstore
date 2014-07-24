package co.codewizards.cloudstore.rest.client.command;

import static co.codewizards.cloudstore.core.util.Util.*;

import javax.ws.rs.core.Response;

public class EndSyncToRepository extends VoidCommand {

	private final String repositoryName;
	private final long fromLocalRevision;

	public EndSyncToRepository(final String repositoryName, final long fromLocalRevision) {
		this.repositoryName = assertNotNull("repositoryName", repositoryName);
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
