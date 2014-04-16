package co.codewizards.cloudstore.core.dto;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class SymlinkDTO extends RepoFileDTO {

	private String target;

	public String getTarget() {
		return target;
	}
	public void setTarget(String target) {
		this.target = target;
	}

}
