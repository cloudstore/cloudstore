package co.codewizards.cloudstore.rest.client.request;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.Date;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import co.codewizards.cloudstore.core.dto.DateTime;

public class MakeDirectory extends VoidRequest {

	protected final String repositoryName;
	protected final String path;
	protected final Date lastModified;

	public MakeDirectory(final String repositoryName, final String path, final Date lastModified) {
		this.repositoryName = assertNotNull(repositoryName, "repositoryName");
		this.path = assertNotNull(path, "path");
		this.lastModified = lastModified;
	}

	@Override
	protected Response _execute() {
		final WebTarget webTarget = createMakeDirectoryWebTarget();
		return assignCredentials(webTarget.request()).post(null);
	}

	protected WebTarget createMakeDirectoryWebTarget() {
//		WebTarget webTarget = client.target(getBaseURL()).path(repositoryName).path(removeLeadingAndTrailingSlash(path));
//
//		if (lastModified != null)
//			webTarget = webTarget.queryParam("lastModified", new DateTime(lastModified));
//
//		Response response = webTarget.request().method("MKCOL");
//		assertResponseIndicatesSuccess(response);

		// The HTTP verb "MKCOL" is not yet supported by Jersey (and not even the unterlying HTTP client)
		// by default. We first have to add this. This will be done later (for the WebDAV support). For
		// now, we'll use the alternative MakeDirectoryService.

		WebTarget webTarget = createWebTarget("_makeDirectory", urlEncode(repositoryName), encodePath(path));

		if (lastModified != null)
			webTarget = webTarget.queryParam("lastModified", new DateTime(lastModified));

		return webTarget;
	}
}
