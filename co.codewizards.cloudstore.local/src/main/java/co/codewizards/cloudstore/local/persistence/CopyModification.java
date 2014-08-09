package co.codewizards.cloudstore.local.persistence;

import static co.codewizards.cloudstore.core.util.HashUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

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
@Discriminator(strategy=DiscriminatorStrategy.VALUE_MAP, value="CopyModification")
@Indices({
	@Index(name="CopyModification_fromPathSha1", members={"fromPathSha1"}),
	@Index(name="CopyModification_toPathSha1", members={"toPathSha1"}),
	@Index(name="CopyModification_sha1_length", members={"sha1", "length"})
})
// Note: It would be nice, but we cannot add unique keys combining localRevision and one of the properties here
// in this sub-class, because there are 2 separate tables (InheritanceStrategy.NEW_TABLE).
@Queries({
	@Query(name="getCopyModificationsForPathAfter_pathSha1_localRevision_remoteRepository", value="SELECT WHERE this.pathSha1 == :pathSha1 && this.localRevision > :localRevision"),
	@Query(name="getCopyModifications_sha1_length", value="SELECT WHERE this.sha1 == :sha1 && this.length == :length")
})
public class CopyModification extends Modification {

	@Persistent(nullValue=NullValue.EXCEPTION, defaultFetchGroup="true")
	@Column(jdbcType="CLOB")
	private String fromPath;

	@Persistent(nullValue=NullValue.EXCEPTION)
	private String fromPathSha1;

	@Persistent(nullValue=NullValue.EXCEPTION, defaultFetchGroup="true")
	@Column(jdbcType="CLOB")
	private String toPath;

	@Persistent(nullValue=NullValue.EXCEPTION)
	private String toPathSha1;

	private long length;

	private String sha1;

	/**
	 * Gets the source path of the copied file.
	 * <p>
	 * This path is always relative to the local repository's root; even if the remote repository uses a
	 * {@link RemoteRepository#getLocalPathPrefix() path-prefix}. Stripping of the path-prefix is
	 * done during Dto generation.
	 * @return the source path of the copied file. Never <code>null</code>.
	 */
	public String getFromPath() {
		return fromPath;
	}
	public void setFromPath(final String fromPath) {
		assertNotNull("fromPath", fromPath);
		if (fromPath.isEmpty())
			throw new IllegalArgumentException("fromPath is empty! fromPath must start with '/' and thus has a minimum length of 1 char!");

		if (!fromPath.startsWith("/"))
			throw new IllegalArgumentException("fromPath does not start with '/'!");

		this.fromPath = fromPath;
		this.fromPathSha1 = sha1(fromPath);
	}

	public String getToPath() {
		return toPath;
	}
	public void setToPath(final String toPath) {
		assertNotNull("toPath", toPath);
		if (toPath.isEmpty())
			throw new IllegalArgumentException("toPath is empty! toPath must start with '/' and thus has a minimum length of 1 char!");

		if (!toPath.startsWith("/"))
			throw new IllegalArgumentException("toPath does not start with '/'!");

		this.toPath = toPath;
		this.toPathSha1 = sha1(toPath);
	}

	public long getLength() {
		return length;
	}
	public void setLength(final long length) {
		this.length = length;
	}
	public String getSha1() {
		return sha1;
	}
	public void setSha1(final String sha1) {
		this.sha1 = sha1;
	}

}
