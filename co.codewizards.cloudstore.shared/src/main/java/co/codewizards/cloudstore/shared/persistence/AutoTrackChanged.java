package co.codewizards.cloudstore.shared.persistence;

import java.util.Date;

public interface AutoTrackChanged {
	/**
	 * Gets the timestamp of when this entity was last changed.
	 * @return the timestamp of when this entity was last changed. Never <code>null</code> in persistence.
	 */
	Date getChanged();
	void setChanged(Date changed);
}
