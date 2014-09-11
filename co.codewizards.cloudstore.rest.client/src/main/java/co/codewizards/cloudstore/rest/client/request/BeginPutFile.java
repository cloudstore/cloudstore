package co.codewizards.cloudstore.rest.client.request;

import javax.ws.rs.core.Response;

import co.codewizards.cloudstore.core.util.AssertUtil;

public class BeginPutFile extends VoidRequest {

	private final String repositoryName;
	private final String path;
	private boolean isInProgress;

	public BeginPutFile(final String repositoryName, final String path, final boolean isInProgress) {
		this.repositoryName = AssertUtil.assertNotNull("repositoryName", repositoryName);
		this.path = path;
		this.isInProgress = isInProgress;
	}

	@Override
	public Response _execute() {
		return assignCredentials(
				createWebTarget("_beginPutFile", urlEncode(repositoryName), encodePath(path), Boolean.toString(isInProgress))
				.request()).post(null);
	}

}
