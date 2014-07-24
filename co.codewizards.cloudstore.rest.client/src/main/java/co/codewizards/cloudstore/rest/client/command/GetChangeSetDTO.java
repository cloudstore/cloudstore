package co.codewizards.cloudstore.rest.client.command;

import static co.codewizards.cloudstore.core.util.Util.*;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import co.codewizards.cloudstore.core.dto.ChangeSetDTO;

public class GetChangeSetDTO extends AbstractCommand<ChangeSetDTO> {

	private final String repositoryName;
	private final boolean localSync;

	public GetChangeSetDTO(final String repositoryName, final boolean localSync) {
		this.repositoryName = assertNotNull("repositoryName", repositoryName);
		this.localSync = localSync;
	}

	@Override
	public ChangeSetDTO execute() {
		WebTarget webTarget = createWebTarget(getPath(ChangeSetDTO.class), urlEncode(repositoryName));

		if (localSync)
			webTarget = webTarget.queryParam("localSync", localSync);

		final ChangeSetDTO changeSetDTO = assignCredentials(webTarget.request(MediaType.APPLICATION_XML)).get(ChangeSetDTO.class);
		return changeSetDTO;
	}

	@Override
	public boolean isResultNullable() {
		return false;
	}

}
