package co.codewizards.cloudstore.rest.client.request;

import static co.codewizards.cloudstore.core.util.Util.*;

import javax.ws.rs.core.Response;

import co.codewizards.cloudstore.core.util.AssertUtil;

public class Copy extends VoidRequest {

	private final String repositoryName;
	private final String fromPath;
	private final String toPath;

	public Copy(final String repositoryName, final String fromPath, final String toPath) {
		this.repositoryName = AssertUtil.assertNotNull("repositoryName", repositoryName);
		this.fromPath = AssertUtil.assertNotNull("fromPath", fromPath);
		this.toPath = AssertUtil.assertNotNull("toPath", toPath);
	}

	@Override
	protected Response _execute() {
		return assignCredentials(createWebTarget("_copy", urlEncode(repositoryName), encodePath(fromPath))
				.queryParam("to", encodePath(toPath))
				.request()).post(null);
	}

}
