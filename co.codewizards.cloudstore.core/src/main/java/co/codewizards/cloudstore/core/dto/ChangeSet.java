package co.codewizards.cloudstore.core.dto;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ChangeSet {
	private RepositoryDTO repositoryDTO;
	private List<ModificationDTO> modificationDTOs;
	private List<RepoFileDTO> repoFileDTOs;

	public RepositoryDTO getRepositoryDTO() {
		return repositoryDTO;
	}
	public void setRepositoryDTO(RepositoryDTO repositoryDTO) {
		this.repositoryDTO = repositoryDTO;
	}

	public List<ModificationDTO> getModificationDTOs() {
		if (modificationDTOs == null)
			modificationDTOs = new ArrayList<ModificationDTO>();

		return modificationDTOs;
	}
	public void setModificationDTOs(List<ModificationDTO> modificationDTOs) {
		this.modificationDTOs = modificationDTOs;
	}

	public List<RepoFileDTO> getRepoFileDTOs() {
		if (repoFileDTOs == null)
			repoFileDTOs = new ArrayList<RepoFileDTO>();

		return repoFileDTOs;
	}
	public void setRepoFileDTOs(List<RepoFileDTO> repoFileDTOs) {
		this.repoFileDTOs = repoFileDTOs;
	}
}
