package co.codewizards.cloudstore.core.dto;

import java.io.File;
import java.util.Date;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
@XmlRootElement
public class RepoFileDto {
	private long id = -1;

	private Long parentId;

	private String name;

//	private long localRevision;

	private Date lastModified;

	public long getId() {
		return id;
	}
	public void setId(final long id) {
		this.id = id;
	}

	public Long getParentId() {
		return parentId;
	}
	public void setParentId(final Long parentId) {
		this.parentId = parentId;
	}

	public String getName() {
		return name;
	}
	public void setName(final String name) {
		this.name = name;
	}

//	public long getLocalRevision() {
//		return localRevision;
//	}
//	public void setLocalRevision(final long localRevision) {
//		this.localRevision = localRevision;
//	}
	/**
	 * Gets the timestamp of the file's last modification.
	 * <p>
	 * It reflects the {@link File#lastModified() File.lastModified} property.
	 * @return the timestamp of the file's last modification.
	 */
	public Date getLastModified() {
		return lastModified;
	}
	public void setLastModified(final Date lastModified) {
		this.lastModified = lastModified;
	}
}
