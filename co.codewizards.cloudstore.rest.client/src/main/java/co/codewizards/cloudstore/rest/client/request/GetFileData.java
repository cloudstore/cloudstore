package co.codewizards.cloudstore.rest.client.request;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

public class GetFileData extends AbstractRequest<byte[]> {
	private final String repositoryName;
	private final String path;
	private final long offset;
	private final int length;

	public GetFileData(final String repositoryName, final String path, final long offset, final int length) {
		this.repositoryName = assertNotNull(repositoryName, "repositoryName");
		this.path = path;
		this.offset = offset;
		this.length = length;
	}

	@Override
	public byte[] execute() {
		WebTarget webTarget = createWebTarget(urlEncode(repositoryName), encodePath(path));

		if (offset > 0) // defaults to 0
			webTarget = webTarget.queryParam("offset", offset);

		if (length >= 0) // defaults to -1 meaning "all"
			webTarget = webTarget.queryParam("length", length);

		return assignCredentials(webTarget.request(MediaType.APPLICATION_OCTET_STREAM)).get(byte[].class);
	}

	@Override
	public boolean isResultNullable() {
		return false;
	}
}
