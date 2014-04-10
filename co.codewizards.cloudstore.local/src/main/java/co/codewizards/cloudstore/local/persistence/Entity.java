package co.codewizards.cloudstore.local.persistence;

import static co.codewizards.cloudstore.core.util.Util.*;

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
public abstract class Entity implements AutoTrackChanged
{
	@PrimaryKey
	@Persistent(valueStrategy=IdGeneratorStrategy.NATIVE)
	private long id = -1;

	// We always initialise this, though the value might be overwritten when DataNucleus loads
	// the object's data from the DB. There's no need to defer the Date instantiation.
	// Creating 1 million instances of Date costs less than 68 ms (I tested creating them and
	// putting them into a LinkedList (preventing optimizer short-cuts), so the LinkedList
	// overhead is included in this time).
	@Persistent(nullValue=NullValue.EXCEPTION)
	private Date created = new Date();

	@Persistent(nullValue=NullValue.EXCEPTION)
	private Date changed = new Date();

	/**
	 * Get the unique identifier of this object.
	 * <p>
	 * This identifier is unique per entity type (the first sub-class of this class
	 * having an own table - which is usually the direct sub-class of {@link Entity}).
	 * <p>
	 * This identifier is assigned when the object is persisted into the DB.
	 * @return the unique identifier of this object.
	 */
	public long getId() {
		return id;
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
	 * @return the timestamp of the creation of this entity. Never <code>null</code>.
	 */
	public Date getCreated() {
		return created;
	}
	/**
	 * Sets the timestamp of the creation of this entity.
	 * <p>
	 * <b>Important: You should normally never invoke this method!</b> The {@code created} property
	 * is supposed to be read-only (assigned once during object creation and never again).
	 * This setter merely exists for extraordinary, unforeseen use cases as well as tests.
	 * @param created the timestamp of the creation of this entity. Must not be <code>null</code>.
	 */
	protected void setCreated(Date created) {
		assertNotNull("created", created);
		this.created = created;
	}

	@Override
	public Date getChanged() {
		return changed;
	}
	@Override
	public void setChanged(Date changed) {
		assertNotNull("created", created);
		this.changed = changed;
	}

}