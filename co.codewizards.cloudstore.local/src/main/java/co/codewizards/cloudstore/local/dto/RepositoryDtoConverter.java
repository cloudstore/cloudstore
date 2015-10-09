package co.codewizards.cloudstore.local.dto;

import static co.codewizards.cloudstore.core.objectfactory.ObjectFactoryUtil.*;
import co.codewizards.cloudstore.core.dto.RepositoryDto;
import co.codewizards.cloudstore.local.persistence.LocalRepository;

public class RepositoryDtoConverter {

	protected RepositoryDtoConverter() {
	}

	public static RepositoryDtoConverter create() {
		return createObject(RepositoryDtoConverter.class);
	}

	public RepositoryDto toRepositoryDto(final LocalRepository localRepository) {
		final RepositoryDto repositoryDto = createObject(RepositoryDto.class);
		repositoryDto.setRepositoryId(localRepository.getRepositoryId());
		repositoryDto.setRevision(localRepository.getRevision());
		repositoryDto.setPublicKey(localRepository.getPublicKey());
		return repositoryDto;
	}
}
