package co.codewizards.cloudstore.shared.persistence;

import java.net.URL;

import javax.jdo.annotations.Discriminator;
import javax.jdo.annotations.DiscriminatorStrategy;
import javax.jdo.annotations.Inheritance;
import javax.jdo.annotations.InheritanceStrategy;
import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;

@PersistenceCapable
@Inheritance(strategy=InheritanceStrategy.SUPERCLASS_TABLE)
@Discriminator(strategy=DiscriminatorStrategy.VALUE_MAP, value="RemoteRepository")
public class RemoteRepository extends Repository {

	@Persistent(nullValue=NullValue.EXCEPTION)
	private URL remoteRoot;

	public URL getRemoteRoot() {
		return remoteRoot;
	}

	public void setRemoteRoot(URL remoteRoot) {
		this.remoteRoot = remoteRoot;
	}

}
