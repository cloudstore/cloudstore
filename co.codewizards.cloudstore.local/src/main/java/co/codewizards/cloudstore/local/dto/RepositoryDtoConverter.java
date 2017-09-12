package co.codewizards.cloudstore.local.dto;

import static co.codewizards.cloudstore.core.objectfactory.ObjectFactoryUtil.*;

import co.codewizards.cloudstore.core.dto.RepositoryDto;
import co.codewizards.cloudstore.local.persistence.Repository;

public class RepositoryDtoConverter {

	protected RepositoryDtoConverter() {
	}

	public static RepositoryDtoConverter create() {
		return createObject(RepositoryDtoConverter.class);
	}

	public RepositoryDto toRepositoryDto(final Repository repository) {
		final RepositoryDto repositoryDto = createObject(RepositoryDto.class);
		repositoryDto.setRepositoryId(repository.getRepositoryId());
		repositoryDto.setRevision(repository.getRevision());
		repositoryDto.setPublicKey(repository.getPublicKey());
		return repositoryDto;
	}
}
