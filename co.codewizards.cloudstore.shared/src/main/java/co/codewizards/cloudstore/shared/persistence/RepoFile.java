package co.codewizards.cloudstore.shared.persistence;

import java.util.Date;

import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.Queries;
import javax.jdo.annotations.Query;
import javax.jdo.annotations.Unique;

@PersistenceCapable
@Unique(name="File_parent_name", members={"parent", "name"})
@Queries({
	@Query(name="getRepoFile_parent_name", value="SELECT UNIQUE WHERE this.parent == :parent && this.name == :name"),
	@Query(name="getChildRepoFiles_parent", value="SELECT WHERE this.parent == :parent")
})
public class RepoFile extends Entity {

	private RepoFile parent;

	@Persistent(nullValue=NullValue.EXCEPTION)
	private String name;

	@Persistent(nullValue=NullValue.EXCEPTION)
	private FileType fileType;

	private long revision;

	private Date lastModified;

	private long size;

	private String sha;

	public RepoFile getParent() {
		return parent;
	}
	public void setParent(RepoFile parent) {
		this.parent = parent;
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	public FileType getFileType() {
		return fileType;
	}
	public void setFileType(FileType fileType) {
		this.fileType = fileType;
	}

	/**
	 * Get the revision of the last modification.
	 * <p>
	 * Note that this does not include modifications of children (in case this is a directory).
	 * If a child is modified, solely this child's revision is updated.
	 * @return the revision of the last modification.
	 */
	public long getRevision() {
		return revision;
	}
	public void setRevision(long revision) {
		this.revision = revision;
	}

	public Date getLastModified() {
		return lastModified;
	}
	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}
	public long getSize() {
		return size;
	}
	public void setSize(long size) {
		this.size = size;
	}
	public String getSha() {
		return sha;
	}
	public void setSha(String sha) {
		this.sha = sha;
	}
}
