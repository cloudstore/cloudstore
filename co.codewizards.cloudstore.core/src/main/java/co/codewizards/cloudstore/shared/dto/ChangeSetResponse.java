package co.codewizards.cloudstore.shared.dto;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ChangeSetResponse {
	private RepositoryDTO repositoryDTO;
	private List<RepoFileDTO> repoFileDTOs;

	public RepositoryDTO getRepositoryDTO() {
		return repositoryDTO;
	}
	public void setRepositoryDTO(RepositoryDTO repositoryDTO) {
		this.repositoryDTO = repositoryDTO;
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
