package co.codewizards.cloudstore.shared.dto;

import java.util.ArrayList;
import java.util.List;

public class StringList {

	private List<String> elements;

	public List<String> getElements() {
		if (elements == null)
			elements = new ArrayList<String>();

		return elements;
	}

	public void setElements(List<String> elements) {
		this.elements = elements;
	}
}
