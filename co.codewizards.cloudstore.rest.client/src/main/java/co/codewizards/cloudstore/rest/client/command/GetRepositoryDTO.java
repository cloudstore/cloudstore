package co.codewizards.cloudstore.rest.client.command;

import static co.codewizards.cloudstore.core.util.Util.*;
import co.codewizards.cloudstore.core.dto.RepositoryDTO;

public class GetRepositoryDTO extends AbstractCommand<RepositoryDTO> {

	private final String repositoryName;

	public GetRepositoryDTO(final String repositoryName) {
		this.repositoryName = assertNotNull("repositoryName", repositoryName);
	}

	@Override
	public RepositoryDTO execute() {
		final RepositoryDTO repositoryDTO = createWebTarget(getPath(RepositoryDTO.class), urlEncode(repositoryName))
				.request().get(RepositoryDTO.class);
		return repositoryDTO;
	}

	@Override
	public boolean isResultNullable() {
		return false;
	}

}
