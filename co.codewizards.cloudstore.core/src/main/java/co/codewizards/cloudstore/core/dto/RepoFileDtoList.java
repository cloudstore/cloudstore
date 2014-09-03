package co.codewizards.cloudstore.core.dto;

import java.util.ArrayList;
import java.util.List;

public class RepoFileDtoList {

	private List<RepoFileDto> elements;

	public List<RepoFileDto> getElements() {
		if (elements == null)
			elements = new ArrayList<RepoFileDto>();

		return elements;
	}

	public void setElements(List<RepoFileDto> elements) {
		this.elements = elements;
	}

}
