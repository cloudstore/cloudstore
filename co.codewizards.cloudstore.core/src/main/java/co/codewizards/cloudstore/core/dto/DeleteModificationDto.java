package co.codewizards.cloudstore.core.dto;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class DeleteModificationDto extends ModificationDto {

	private String path;

	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "[id=" + getId()
				+ ", localRevision=" + getLocalRevision()
				+ ", path=" + path
				+ "]";
	}
}
