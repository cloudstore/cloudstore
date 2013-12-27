package co.codewizards.cloudstore.shared.persistence;

import java.io.File;
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
public class RepoFile extends Entity implements AutoTrackLocalRevision {

	private RepoFile parent;

	@Persistent(nullValue=NullValue.EXCEPTION)
	private String name;

	@Persistent(nullValue=NullValue.EXCEPTION)
	private FileType fileType;

	private long localRevision;

	private Date lastModified;

	private long length;

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
	 * {@inheritDoc}
	 * <p>
	 * Note that this does not include modifications of children (in case this is a directory).
	 * If a child is modified, solely this child's localRevision is updated.
	 */
	@Override
	public long getLocalRevision() {
		return localRevision;
	}
	@Override
	public void setLocalRevision(long revision) {
		this.localRevision = revision;
	}

	/**
	 * Gets the timestamp of the file's last modification.
	 * <p>
	 * It reflects the {@link File#lastModified() File.lastModified} property.
	 * @return
	 */
	public Date getLastModified() {
		return lastModified;
	}
	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}
	public long getLength() {
		return length;
	}
	public void setLength(long size) {
		this.length = size;
	}
	public String getSha() {
		return sha;
	}
	public void setSha(String sha) {
		this.sha = sha;
	}
}
