package co.codewizards.cloudstore.core.persistence;

import static co.codewizards.cloudstore.core.util.HashUtil.sha1;
import static co.codewizards.cloudstore.core.util.Util.assertNotNull;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.Discriminator;
import javax.jdo.annotations.DiscriminatorStrategy;
import javax.jdo.annotations.Index;
import javax.jdo.annotations.Indices;
import javax.jdo.annotations.Inheritance;
import javax.jdo.annotations.InheritanceStrategy;
import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.Queries;
import javax.jdo.annotations.Query;

@PersistenceCapable
@Inheritance(strategy=InheritanceStrategy.NEW_TABLE)
@Discriminator(strategy=DiscriminatorStrategy.VALUE_MAP, value="DeleteModification")
@Indices({
	@Index(name="DeleteModification_pathSha1", members={"pathSha1"}),
	@Index(name="DeleteModification_sha1_length", members={"sha1", "length"})
})
//@Unique(name="DeleteModification_pathSha1_localRevision_remoteRepository", members={"pathSha1", "localRevision", "remoteRepository"}) // causes an NPE :-( The NPE is not nice, but it's clear that this cannot work: There are 2 separate tables (InheritanceStrategy.NEW_TABLE).
@Queries({
	@Query(name="getDeleteModificationsForPathAfter_pathSha1_localRevision_remoteRepository", value="SELECT WHERE this.pathSha1 == :pathSha1 && this.localRevision > :localRevision"),
	@Query(name="getDeleteModifications_sha1_length", value="SELECT WHERE this.sha1 == :sha1 && this.length == :length")
})
public class DeleteModification extends Modification {

	@Persistent(nullValue=NullValue.EXCEPTION, defaultFetchGroup="true")
	@Column(jdbcType="CLOB")
	private String path;

	@Persistent(nullValue=NullValue.EXCEPTION)
	private String pathSha1;

	private long length;

	private String sha1;

	/**
	 * Gets the path of the deleted directory or file.
	 * <p>
	 * This path is always relative to the local repository's root; even if the remote repository uses a
	 * {@link RemoteRepository#getLocalPathPrefix() path-prefix}. Stripping of the path-prefix is
	 * done during DTO generation.
	 * @return the path of the deleted directory or file. Never <code>null</code>.
	 */
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		assertNotNull("path", path);
		if (path.isEmpty())
			throw new IllegalArgumentException("path is empty! path must start with '/' and thus has a minimum length of 1 char!");

		if (!path.startsWith("/"))
			throw new IllegalArgumentException("path does not start with '/'!");

		this.path = path;
		this.pathSha1 = sha1(path);
	}

	public long getLength() {
		return length;
	}
	public void setLength(long length) {
		this.length = length;
	}
	public String getSha1() {
		return sha1;
	}
	public void setSha1(String sha1) {
		this.sha1 = sha1;
	}

}
