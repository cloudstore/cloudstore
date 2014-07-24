package co.codewizards.cloudstore.rest.client.command;

import static co.codewizards.cloudstore.core.util.Util.*;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class PutFileData extends VoidCommand {
	private final String repositoryName;
	private final String path;
	private final long offset;
	private final byte[] fileData;

	public PutFileData(final String repositoryName, final String path, final long offset, final byte[] fileData) {
		this.repositoryName = assertNotNull("repositoryName", repositoryName);
		this.path = assertNotNull("path", path);
		this.offset = offset;
		this.fileData = assertNotNull("fileData", fileData);
	}

	@Override
	protected Response _execute() {
		WebTarget webTarget = createWebTarget(urlEncode(repositoryName), encodePath(path));

		if (offset > 0)
			webTarget = webTarget.queryParam("offset", offset);

		return assignCredentials(webTarget.request()).put(Entity.entity(fileData, MediaType.APPLICATION_OCTET_STREAM));
	}
}
