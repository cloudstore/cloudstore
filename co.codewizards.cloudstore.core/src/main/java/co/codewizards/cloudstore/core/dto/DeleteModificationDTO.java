package co.codewizards.cloudstore.core.dto;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class DeleteModificationDTO extends ModificationDTO {

	private String path;

	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}

}
