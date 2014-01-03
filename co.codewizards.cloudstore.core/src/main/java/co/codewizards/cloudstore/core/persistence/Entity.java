package co.codewizards.cloudstore.core.persistence;

import java.util.Date;
import java.util.UUID;

import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.Inheritance;
import javax.jdo.annotations.InheritanceStrategy;
import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import co.codewizards.cloudstore.core.dto.EntityID;

/**
 * Base class of all persistence-capable (a.k.a. entity) classes.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
@PersistenceCapable(
		identityType=IdentityType.APPLICATION,
		objectIdClass=EntityID.class
)
@Inheritance(strategy=InheritanceStrategy.SUBCLASS_TABLE)
public class Entity implements AutoTrackChanged {

	@PrimaryKey
	private long idHigh;

	@PrimaryKey
	private long idLow;

	@Persistent(nullValue=NullValue.EXCEPTION)
	private Date created = new Date();

	@Persistent(nullValue=NullValue.EXCEPTION)
	private Date changed = new Date();

	public long getIdHigh() {
		return idHigh;
	}

	public long getIdLow() {
		return idLow;
	}

	public EntityID getEntityID()
	{
		return new EntityID(idHigh, idLow);
	}

	public Entity()
	{
		this(null);
	}

	protected Entity(EntityID entityID)
	{
		if (entityID == null) {
			UUID uuid = UUID.randomUUID();
			idHigh = uuid.getMostSignificantBits();
			idLow = uuid.getLeastSignificantBits();
		}
		else {
			idHigh = entityID.idHigh;
			idLow = entityID.idLow;
		}
	}

	@Override
	public boolean equals(Object other)
	{
		if (other == this)
			return true;

		if (other == null)
			return false;

		if (this.getClass() != other.getClass())
			return false;

		Entity o = (Entity) other;
		return this.idHigh == o.idHigh && this.idLow == o.idLow;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (idHigh ^ (idHigh >>> 32));
		result = prime * result + (int) (idLow ^ (idLow >>> 32));
		return result;
	}

	@Override
	public String toString() {
		return super.toString() + '[' + new UUID(idHigh, idLow) + ']';
	}

//	@Override
//	public boolean equals(Object obj) {
//		if (this == obj) {
//			return true;
//		}
//		if (obj == null) {
//			return false;
//		}
//
//		Object thisOid = JDOHelper.getObjectId(this);
//		if (thisOid == null) {
//			return false;
//		}
//
//		Object otherOid = JDOHelper.getObjectId(obj);
//		return thisOid.equals(otherOid);
//	}
//
//	@Override
//	public int hashCode() {
//		Object thisOid = JDOHelper.getObjectId(this);
//		if (thisOid == null) {
//			return super.hashCode();
//		}
//		return thisOid.hashCode();
//	}
//
//	@Override
//	public String toString() {
//		return String.format("%s[%s]", getClass().getSimpleName(), JDOHelper.getObjectId(this));
//	}

	/**
	 * Gets the timestamp of the creation of this entity.
	 * @return the timestamp of the creation of this entity. Never <code>null</code> in persistence.
	 */
	public Date getCreated() {
		return created;
	}
	public void setCreated(Date created) {
		this.created = created;
	}
	/**
	 * Gets the timestamp of when this entity was last changed.
	 * @return the timestamp of when this entity was last changed. Never <code>null</code> in persistence.
	 */
	@Override
	public Date getChanged() {
		return changed;
	}
	@Override
	public void setChanged(Date changed) {
		this.changed = changed;
	}
}