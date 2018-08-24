package co.codewizards.cloudstore.core.dto;

import java.io.Serializable;
import java.util.Date;

import javax.xml.bind.annotation.XmlRootElement;

import co.codewizards.cloudstore.core.oio.File;

/**
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
@SuppressWarnings("serial")
@XmlRootElement
public class RepoFileDto implements Serializable {
	private long id = Long.MIN_VALUE;

	private Long parentId;

	private String name;

	private long localRevision;

	private Date lastModified;

	private boolean neededAsParent;

	public RepoFileDto() { }

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

	public long getLocalRevision() {
		return localRevision;
	}
	public void setLocalRevision(final long localRevision) {
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
	public void setLastModified(final Date lastModified) {
		this.lastModified = lastModified;
	}

	/**
	 * Indicates, whether this {@link RepoFileDto} was added to a {@link ChangeSetDto}, because it was needed
	 * as parent.
	 * <p>
	 * If this is <code>true</code>, the underlying file/directory is not dirty and does thus not need
	 * to be transferred. The presence of this {@code ChangeSetDto} serves only to complete the tree structure.
	 * <p>
	 * If this is <code>false</code>, the underlying file/directory was modified and must be transferred.
	 * @return whether this instance is only a filler to complete the tree, and the underlying file/directory was not modified.
	 */
	public boolean isNeededAsParent() {
		return neededAsParent;
	}

	public void setNeededAsParent(final boolean neededAsParent) {
		this.neededAsParent = neededAsParent;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + '[' + toString_getProperties() + ']';
	}

	protected String toString_getProperties() {
		return "id=" + id
				+ ", parentId=" + parentId
				+ ", name=" + name
				+ ", localRevision=" + localRevision
				+ ", lastModified=" + lastModified
				+ ", neededAsParent=" + neededAsParent;
	}
}
