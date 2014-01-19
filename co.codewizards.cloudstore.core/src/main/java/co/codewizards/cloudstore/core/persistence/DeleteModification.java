package co.codewizards.cloudstore.core.persistence;

import static co.codewizards.cloudstore.core.util.Util.*;

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
	@Index(name="DeleteModification_path", members={"path"}),
	@Index(name="DeleteModification_sha1_length", members={"sha1", "length"})
})
//@Unique(name="DeleteModification_path_localRevision_remoteRepository", members={"path", "localRevision", "remoteRepository"}) // causes an NPE :-(
@Queries({
	@Query(name="getDeleteModificationsForPathAfter_path_localRevision_remoteRepository", value="SELECT WHERE this.path == :path && this.localRevision > :localRevision"),
	@Query(name="getDeleteModifications_sha1_length", value="SELECT WHERE this.sha1 == :sha1 && this.length == :length")
})
public class DeleteModification extends Modification {

	@Persistent(nullValue=NullValue.EXCEPTION)
	private String path;

	private long length;

	private String sha1;

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
