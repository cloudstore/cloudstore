package co.codewizards.cloudstore.core.repo.local;

import java.util.List;

import co.codewizards.cloudstore.core.dto.RepoFileDto;

public interface LocalRepoMetaData {

	RepoFileDto getRepoFileDto(String path, int depth);

	List<RepoFileDto> getChildRepoFileDtos(String path, int depth);

	List<RepoFileDto> getChildRepoFileDtos(long repoFileId, int depth);

}
