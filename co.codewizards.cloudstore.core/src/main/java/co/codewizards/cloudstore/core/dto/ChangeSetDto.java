package co.codewizards.cloudstore.core.dto;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ChangeSetDto {
	private RepositoryDto repositoryDto;
	private List<ModificationDto> modificationDtos;
	private List<RepoFileDto> repoFileDtos;
	private ConfigPropSetDto parentConfigPropSetDto;

	public RepositoryDto getRepositoryDto() {
		return repositoryDto;
	}
	public void setRepositoryDto(RepositoryDto repositoryDto) {
		this.repositoryDto = repositoryDto;
	}

	public List<ModificationDto> getModificationDtos() {
		if (modificationDtos == null)
			modificationDtos = new ArrayList<ModificationDto>();

		return modificationDtos;
	}
	public void setModificationDtos(List<ModificationDto> modificationDtos) {
		this.modificationDtos = modificationDtos;
	}

	public List<RepoFileDto> getRepoFileDtos() {
		if (repoFileDtos == null)
			repoFileDtos = new ArrayList<RepoFileDto>();

		return repoFileDtos;
	}
	public void setRepoFileDtos(List<RepoFileDto> repoFileDtos) {
		this.repoFileDtos = repoFileDtos;
	}

	public ConfigPropSetDto getParentConfigPropSetDto() {
		return parentConfigPropSetDto;
	}
	public void setParentConfigPropSetDto(ConfigPropSetDto configPropSetDto) {
		this.parentConfigPropSetDto = configPropSetDto;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "[repositoryDto=" + repositoryDto
				+ ", repoFileDtos=" + repoFileDtos
				+ ", modificationDtos=" + modificationDtos
				+ ", parentConfigPropSetDto=" + parentConfigPropSetDto
				+ "]";
	}
}
