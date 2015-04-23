package co.codewizards.cloudstore.core.dto;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class SymlinkDto extends RepoFileDto {

	private String target;

	public SymlinkDto() { }

	public String getTarget() {
		return target;
	}
	public void setTarget(final String target) {
		this.target = target;
	}
}
