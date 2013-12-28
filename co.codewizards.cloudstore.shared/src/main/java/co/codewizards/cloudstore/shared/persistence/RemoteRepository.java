package co.codewizards.cloudstore.shared.persistence;

import java.net.URL;

import javax.jdo.annotations.Discriminator;
import javax.jdo.annotations.DiscriminatorStrategy;
import javax.jdo.annotations.Index;
import javax.jdo.annotations.Inheritance;
import javax.jdo.annotations.InheritanceStrategy;
import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;

import co.codewizards.cloudstore.shared.dto.EntityID;

@PersistenceCapable
@Inheritance(strategy=InheritanceStrategy.SUPERCLASS_TABLE)
@Discriminator(strategy=DiscriminatorStrategy.VALUE_MAP, value="RemoteRepository")
@Index(name="RemoteRepository_remoteRoot", members="remoteRoot")
public class RemoteRepository extends Repository {

	@Persistent(nullValue=NullValue.EXCEPTION)
	private URL remoteRoot;

	public RemoteRepository() { }

	public RemoteRepository(EntityID entityID) {
		super(entityID);
	}

	public URL getRemoteRoot() {
		return remoteRoot;
	}

	public void setRemoteRoot(URL remoteRoot) {
		this.remoteRoot = remoteRoot;
	}

}
