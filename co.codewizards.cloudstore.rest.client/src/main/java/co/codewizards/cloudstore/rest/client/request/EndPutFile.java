package co.codewizards.cloudstore.rest.client.request;

import static java.util.Objects.*;

import javax.ws.rs.core.Response;

import co.codewizards.cloudstore.core.dto.DateTime;

public class EndPutFile extends VoidRequest {

	protected final String repositoryName;
	protected final String path;
	protected final DateTime lastModified;
	protected final long length;
	protected final String sha1;

	public EndPutFile(final String repositoryName, final String path, final DateTime lastModified, final long length, final String sha1) {
		this.repositoryName = requireNonNull(repositoryName, "repositoryName");
		this.path = requireNonNull(path, "path");
		this.lastModified = requireNonNull(lastModified, "lastModified");
		this.length = length;
		this.sha1 = sha1;
	}

	@Override
	public Response _execute() {
		return assignCredentials(
				createWebTarget("_endPutFile", urlEncode(repositoryName), encodePath(path))
				.queryParam("lastModified", lastModified.toString())
				.queryParam("length", length)
				.queryParam("sha1", sha1)
				.request()).put(null);
	}

}
