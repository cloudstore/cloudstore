package co.codewizards.cloudstore.rest.client.request;

import static co.codewizards.cloudstore.core.util.Util.*;

import javax.ws.rs.core.Response;

import co.codewizards.cloudstore.core.util.AssertUtil;

public class EndSyncToRepository extends VoidRequest {

	private final String repositoryName;
	private final long fromLocalRevision;

	public EndSyncToRepository(final String repositoryName, final long fromLocalRevision) {
		this.repositoryName = AssertUtil.assertNotNull("repositoryName", repositoryName);
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
