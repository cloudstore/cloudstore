package co.codewizards.cloudstore.core.dto;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ListDto<E> {

	private List<E> elements;

	public List<E> getElements() {
		if (elements == null)
			elements = new ArrayList<>();

		return elements;
	}
	public void setElements(List<E> elements) {
		this.elements = elements;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + (elements == null ? "[]" : elements.toString());
	}
}
