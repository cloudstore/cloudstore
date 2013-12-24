package co.codewizards.cloudstore.shared.persistence;

import javax.jdo.JDOHelper;
import javax.jdo.annotations.Inheritance;
import javax.jdo.annotations.InheritanceStrategy;
import javax.jdo.annotations.PersistenceCapable;

@PersistenceCapable
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
}
