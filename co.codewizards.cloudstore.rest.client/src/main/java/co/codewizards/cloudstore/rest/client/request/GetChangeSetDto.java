package co.codewizards.cloudstore.rest.client.request;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import co.codewizards.cloudstore.core.dto.ChangeSetDto;

public class GetChangeSetDto extends AbstractRequest<ChangeSetDto> {

	private final String repositoryName;
	private final boolean localSync;

	public GetChangeSetDto(final String repositoryName, final boolean localSync) {
		this.repositoryName = assertNotNull(repositoryName, "repositoryName");
		this.localSync = localSync;
	}

	@Override
	public ChangeSetDto execute() {
		WebTarget webTarget = createWebTarget(getPath(ChangeSetDto.class), urlEncode(repositoryName));

		if (localSync)
			webTarget = webTarget.queryParam("localSync", localSync);

		final ChangeSetDto changeSetDto = assignCredentials(webTarget.request(MediaType.APPLICATION_XML)).get(ChangeSetDto.class);
		return changeSetDto;
	}

	@Override
	public boolean isResultNullable() {
		return false;
	}

}
