package co.codewizards.cloudstore.shared.dto;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class DirectoryDTO extends RepoFileDTO {

	private List<String> childNames;

	public List<String> getChildNames() {
		if (childNames == null)
			childNames = new ArrayList<String>();

		return childNames;
	}

	public void setChildNames(List<String> childNames) {
		this.childNames = childNames;
	}

}
