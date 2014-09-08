package co.codewizards.cloudstore.core.dto;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;


/**
 * @author Sebastian Schefczyk
 */
@XmlRootElement
public class FileInProgressMarkDtoList {

	private List<FileInProgressMarkDto> elements;

	public List<FileInProgressMarkDto> getElements() {
		if (elements == null)
			elements = new ArrayList<FileInProgressMarkDto>();

		return elements;
	}

	public void setElements(final List<FileInProgressMarkDto> elements) {
		this.elements = elements;
	}

}
