package co.codewizards.cloudstore.shared.persistence;

import java.util.UUID;

import javax.jdo.annotations.Discriminator;
import javax.jdo.annotations.DiscriminatorStrategy;
import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;

@PersistenceCapable
@Discriminator(strategy=DiscriminatorStrategy.VALUE_MAP)
public abstract class Repository extends Entity
{
	@Persistent(nullValue=NullValue.EXCEPTION)
	private UUID uuid;

	public UUID getUuid() {
		return uuid;
	}

	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}
}
