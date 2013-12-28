package co.codewizards.cloudstore.shared.dto;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
@XmlRootElement
public class RepoFileDTO {
	private EntityID entityID;

	private String name;

	public EntityID getEntityID() {
		return entityID;
	}
	public void setEntityID(EntityID entityID) {
		this.entityID = entityID;
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
}
