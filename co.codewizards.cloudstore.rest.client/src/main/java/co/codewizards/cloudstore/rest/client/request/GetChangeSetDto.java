package co.codewizards.cloudstore.rest.client.request;

import static java.util.Objects.*;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import co.codewizards.cloudstore.core.dto.ChangeSetDto;

public class GetChangeSetDto extends AbstractRequest<ChangeSetDto> {

	private final String repositoryName;
	private final boolean localSync;
	private final Long lastSyncToRemoteRepoLocalRepositoryRevisionSynced;

	public GetChangeSetDto(final String repositoryName, final boolean localSync, final Long lastSyncToRemoteRepoLocalRepositoryRevisionSynced) {
		this.repositoryName = requireNonNull(repositoryName, "repositoryName");
		this.localSync = localSync;
		this.lastSyncToRemoteRepoLocalRepositoryRevisionSynced = lastSyncToRemoteRepoLocalRepositoryRevisionSynced;
	}

	@Override
	public ChangeSetDto execute() {
		WebTarget webTarget = createWebTarget(getPath(ChangeSetDto.class), urlEncode(repositoryName));

		if (localSync)
			webTarget = webTarget.queryParam("localSync", localSync);

		if (lastSyncToRemoteRepoLocalRepositoryRevisionSynced != null)
			webTarget = webTarget.queryParam("lastSyncToRemoteRepoLocalRepositoryRevisionSynced", lastSyncToRemoteRepoLocalRepositoryRevisionSynced);

		final ChangeSetDto changeSetDto = assignCredentials(webTarget.request(MediaType.APPLICATION_XML)).get(ChangeSetDto.class);
		return changeSetDto;
	}

	@Override
	public boolean isResultNullable() {
		return false;
	}

}
