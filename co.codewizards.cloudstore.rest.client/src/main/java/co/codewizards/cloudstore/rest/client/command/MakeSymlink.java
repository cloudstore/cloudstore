package co.codewizards.cloudstore.rest.client.command;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.util.Date;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import co.codewizards.cloudstore.core.dto.DateTime;

public class MakeSymlink extends VoidCommand {

	private final String repositoryName;
	private final String path;
	private final String target;
	private final Date lastModified;

	public MakeSymlink(final String repositoryName, final String path, final String target, final Date lastModified) {
		this.repositoryName = assertNotNull("repositoryName", repositoryName);
		this.path = assertNotNull("path", path);
		this.target = assertNotNull("target", target);
		this.lastModified = lastModified;
	}

	@Override
	protected Response _execute() {
		WebTarget webTarget = createWebTarget("_makeSymlink", urlEncode(repositoryName), encodePath(path))
				.queryParam("target", encodePath(target));

		if (lastModified != null)
			webTarget = webTarget.queryParam("lastModified", new DateTime(lastModified));

		return assignCredentials(webTarget.request()).post(null);
	}

}
