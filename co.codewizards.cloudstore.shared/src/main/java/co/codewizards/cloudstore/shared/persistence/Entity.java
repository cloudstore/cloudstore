package co.codewizards.cloudstore.shared.persistence;

import javax.jdo.JDOHelper;
import javax.jdo.annotations.Inheritance;
import javax.jdo.annotations.InheritanceStrategy;
import javax.jdo.annotations.PersistenceCapable;

/**
 * Base class of all persistence-capable (a.k.a. entity) classes.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
@PersistenceCapable(detachable="true")
@Inheritance(strategy=InheritanceStrategy.SUBCLASS_TABLE)
public class Entity {

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
}
