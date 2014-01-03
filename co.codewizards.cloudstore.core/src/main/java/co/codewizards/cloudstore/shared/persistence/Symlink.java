package co.codewizards.cloudstore.shared.persistence;

import javax.jdo.annotations.Discriminator;
import javax.jdo.annotations.DiscriminatorStrategy;
import javax.jdo.annotations.Inheritance;
import javax.jdo.annotations.InheritanceStrategy;
import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;

@PersistenceCapable
@Inheritance(strategy=InheritanceStrategy.SUPERCLASS_TABLE)
@Discriminator(strategy=DiscriminatorStrategy.VALUE_MAP, value="Symlink")
public class Symlink extends RepoFile {

	@Persistent(nullValue=NullValue.EXCEPTION)
	private String target;

	public String getTarget() {
		return target;
	}
	public void setTarget(String target) {
		this.target = target;
	}

}
