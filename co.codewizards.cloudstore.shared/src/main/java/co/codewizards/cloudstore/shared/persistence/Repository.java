package co.codewizards.cloudstore.shared.persistence;

import java.net.URL;
import java.util.UUID;

import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;

@PersistenceCapable
public class Repository extends Entity
{
	@Persistent(nullValue=NullValue.EXCEPTION)
	private UUID uuid;
	private URL remoteRoot;

	public UUID getUuid() {
		return uuid;
	}

	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}

	public URL getRemoteRoot() {
		return remoteRoot;
	}

	public void setRemoteRoot(URL remoteRoot) {
		this.remoteRoot = remoteRoot;
	}
}
