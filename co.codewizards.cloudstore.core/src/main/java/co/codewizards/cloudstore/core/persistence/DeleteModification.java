package co.codewizards.cloudstore.core.persistence;

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
	@Index(name="DeleteModification_sha1_length", members={"sha1", "length"})
})
@Queries({
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
