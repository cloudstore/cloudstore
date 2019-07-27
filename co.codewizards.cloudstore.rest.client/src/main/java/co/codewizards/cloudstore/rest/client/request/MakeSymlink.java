package co.codewizards.cloudstore.rest.client.request;

import static java.util.Objects.*;

import java.util.Date;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import co.codewizards.cloudstore.core.dto.DateTime;

public class MakeSymlink extends VoidRequest {

	protected final String repositoryName;
	protected final String path;
	protected final String target;
	protected final Date lastModified;

	public MakeSymlink(final String repositoryName, final String path, final String target, final Date lastModified) {
		this.repositoryName = requireNonNull(repositoryName, "repositoryName");
		this.path = requireNonNull(path, "path");
		this.target = requireNonNull(target, "target");
		this.lastModified = lastModified;
	}

	@Override
	protected Response _execute() {
		final WebTarget webTarget = createMakeSymlinkWebTarget();
		return assignCredentials(webTarget.request()).post(null);
	}

	protected WebTarget createMakeSymlinkWebTarget() {
		WebTarget webTarget = createWebTarget("_makeSymlink", urlEncode(repositoryName), encodePath(path))
				.queryParam("target", encodePath(target));

		if (lastModified != null)
			webTarget = webTarget.queryParam("lastModified", new DateTime(lastModified));

		return webTarget;
	}
}
