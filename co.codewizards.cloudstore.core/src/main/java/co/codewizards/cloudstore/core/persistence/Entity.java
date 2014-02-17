package co.codewizards.cloudstore.core.persistence;

import java.util.Date;

import javax.jdo.JDOHelper;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.Inheritance;
import javax.jdo.annotations.InheritanceStrategy;
import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

/**
 * Base class of all persistence-capable (a.k.a. entity) classes.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
@PersistenceCapable(identityType=IdentityType.APPLICATION)
@Inheritance(strategy=InheritanceStrategy.SUBCLASS_TABLE)
public class Entity implements AutoTrackChanged
{
	@PrimaryKey
	@Persistent(valueStrategy=IdGeneratorStrategy.NATIVE)
	private long id;

	@Persistent(nullValue=NullValue.EXCEPTION)
	private Date created = new Date();

	@Persistent(nullValue=NullValue.EXCEPTION)
	private Date changed = new Date();

	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}

		Object thisOid = JDOHelper.getObjectId(this);
		if (thisOid == null) {
			return false;
		}

		Object otherOid = JDOHelper.getObjectId(obj);
		return thisOid.equals(otherOid);
	}

	@Override
	public int hashCode() {
		Object thisOid = JDOHelper.getObjectId(this);
		if (thisOid == null) {
			return super.hashCode();
		}
		return thisOid.hashCode();
	}

	@Override
	public String toString() {
		return String.format("%s[%s]", getClass().getSimpleName(), JDOHelper.getObjectId(this));
	}

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