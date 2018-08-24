package co.codewizards.cloudstore.local.persistence;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.Date;

import javax.jdo.JDOHelper;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.Inheritance;
import javax.jdo.annotations.InheritanceStrategy;
import javax.jdo.annotations.NotPersistent;
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
	private long id = Long.MIN_VALUE;

	// We always initialise this, though the value might be overwritten when DataNucleus loads
	// the object's data from the DB. There's no need to defer the Date instantiation.
	// Creating 1 million instances of Date costs less than 68 ms (I tested creating them and
	// putting them into a LinkedList (preventing optimizer short-cuts), so the LinkedList
	// overhead is included in this time).
	@Persistent(nullValue=NullValue.EXCEPTION)
	private Date created = new Date();

	@Persistent(nullValue=NullValue.EXCEPTION)
	private Date changed = new Date();

	@NotPersistent
	private transient int hashCode;

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
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}

		final Object thisOid = JDOHelper.getObjectId(this);
		if (thisOid == null) {
			return false;
		}

		final Object otherOid = JDOHelper.getObjectId(obj);
		return thisOid.equals(otherOid);
	}

	@Override
	public int hashCode() {
		if (hashCode == 0) {
			// Freeze the hashCode.
			//
			// We make sure the hashCode does not change after it was once initialised, because the object might
			// have been added to a HashSet (or Map) before being persisted. During persistence, the object's id is
			// assigned and without freezing the hashCode, the object is thus not found in the Map/Set, anymore.
			//
			// This new strategy seems to be working well; messages like this do not occur anymore:
			//
			// Aug 22, 2014 9:44:23 AM org.datanucleus.store.rdbms.mapping.java.PersistableMapping postInsert
			// INFO: Object "co.codewizards.cloudstore.local.persistence.FileChunk@36dccbc7" has field "co.codewizards.cloudstore.local.persistence.FileChunk.normalFile" with an N-1 bidirectional relation set to relate to "co.codewizards.cloudstore.local.persistence.NormalFile@63323bb" but the collection at "co.codewizards.cloudstore.local.persistence.NormalFile.fileChunks" doesnt contain this object.

			final Object thisOid = JDOHelper.getObjectId(this);
			if (thisOid == null)
				hashCode = super.hashCode();
			else
				hashCode = thisOid.hashCode();

			if (hashCode == 0) // very unlikely, but we want our code to be 100% robust.
				hashCode = 1;
		}
		return hashCode;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + '[' + toString_getProperties() + ']';
	}

	protected String toString_getProperties() {
		return "id=" + JDOHelper.getObjectId(this);
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
	protected void setCreated(final Date created) {
		assertNotNull(created, "created");
		this.created = created;
	}

	@Override
	public Date getChanged() {
		return changed;
	}
	@Override
	public void setChanged(final Date changed) {
		assertNotNull(created, "created");
		this.changed = changed;
	}

}
