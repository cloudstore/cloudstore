package co.codewizards.cloudstore.core.dto;

import java.io.File;
import java.util.Date;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
@XmlRootElement
public class RepoFileDTO {
	private EntityID entityID;

	private EntityID parentEntityID;

	private String name;

	private long localRevision;

	private Date lastModified;

	public EntityID getEntityID() {
		return entityID;
	}
	public void setEntityID(EntityID entityID) {
		this.entityID = entityID;
	}

	public EntityID getParentEntityID() {
		return parentEntityID;
	}
	public void setParentEntityID(EntityID parentEntityID) {
		this.parentEntityID = parentEntityID;
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	public long getLocalRevision() {
		return localRevision;
	}
	public void setLocalRevision(long localRevision) {
		this.localRevision = localRevision;
	}
	/**
	 * Gets the timestamp of the file's last modification.
	 * <p>
	 * It reflects the {@link File#lastModified() File.lastModified} property.
	 * @return the timestamp of the file's last modification.
	 */
	public Date getLastModified() {
		return lastModified;
	}
	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}
}
