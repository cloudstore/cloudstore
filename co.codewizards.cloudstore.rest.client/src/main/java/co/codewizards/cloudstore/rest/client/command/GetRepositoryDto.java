package co.codewizards.cloudstore.rest.client.command;

import static co.codewizards.cloudstore.core.util.Util.*;
import co.codewizards.cloudstore.core.dto.RepositoryDto;

public class GetRepositoryDto extends AbstractCommand<RepositoryDto> {

	private final String repositoryName;

	public GetRepositoryDto(final String repositoryName) {
		this.repositoryName = assertNotNull("repositoryName", repositoryName);
	}

	@Override
	public RepositoryDto execute() {
		final RepositoryDto repositoryDto = createWebTarget(getPath(RepositoryDto.class), urlEncode(repositoryName))
				.request().get(RepositoryDto.class);
		return repositoryDto;
	}

	@Override
	public boolean isResultNullable() {
		return false;
	}

}
