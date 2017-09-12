package co.codewizards.cloudstore.rest.client.request;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import co.codewizards.cloudstore.core.dto.RepositoryDto;

public class GetClientRepositoryDto extends AbstractRequest<RepositoryDto> {

	private final String repositoryName;

	public GetClientRepositoryDto(final String repositoryName) {
		this.repositoryName = assertNotNull(repositoryName, "repositoryName");
	}

	@Override
	public RepositoryDto execute() {
		final RepositoryDto repositoryDto = assignCredentials(
				createWebTarget("_getClientRepositoryDto", urlEncode(repositoryName)).request())
				.get(RepositoryDto.class);
		return repositoryDto;
	}

	@Override
	public boolean isResultNullable() {
		return false;
	}
}
