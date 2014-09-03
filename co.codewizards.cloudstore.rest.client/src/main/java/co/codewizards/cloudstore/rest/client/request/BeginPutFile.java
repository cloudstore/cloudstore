package co.codewizards.cloudstore.rest.client.request;

import static co.codewizards.cloudstore.core.util.Util.*;

import javax.ws.rs.core.Response;

import co.codewizards.cloudstore.core.util.AssertUtil;

public class BeginPutFile extends VoidRequest {

	private final String repositoryName;
	private final String path;

	public BeginPutFile(final String repositoryName, final String path) {
		this.repositoryName = AssertUtil.assertNotNull("repositoryName", repositoryName);
		this.path = path;
	}

	@Override
	public Response _execute() {
		return assignCredentials(
				createWebTarget("_beginPutFile", urlEncode(repositoryName), encodePath(path))
				.request()).post(null);
	}

}
