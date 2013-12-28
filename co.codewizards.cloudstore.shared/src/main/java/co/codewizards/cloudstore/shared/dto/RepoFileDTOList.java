package co.codewizards.cloudstore.shared.dto;

import java.util.ArrayList;
import java.util.List;

public class RepoFileDTOList {

	private List<RepoFileDTO> elements;

	public List<RepoFileDTO> getElements() {
		if (elements == null)
			elements = new ArrayList<RepoFileDTO>();

		return elements;
	}

}
