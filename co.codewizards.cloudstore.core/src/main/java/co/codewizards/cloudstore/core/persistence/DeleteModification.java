package co.codewizards.cloudstore.core.persistence;

import javax.jdo.annotations.Discriminator;
import javax.jdo.annotations.DiscriminatorStrategy;
import javax.jdo.annotations.Inheritance;
import javax.jdo.annotations.InheritanceStrategy;
import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;

@PersistenceCapable
@Inheritance(strategy=InheritanceStrategy.NEW_TABLE)
@Discriminator(strategy=DiscriminatorStrategy.VALUE_MAP, value="DeleteModification")
public class DeleteModification extends Modification {

	@Persistent(nullValue=NullValue.EXCEPTION)
	private String path;

	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}

}
