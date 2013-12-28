package co.codewizards.cloudstore.shared.persistence;

import java.util.Date;

/**
 * Automatically track changes by updating the {@link #getChanged() changed} property whenever
 * the object is written to the datastore.
 * <p>
 * This interface is implemented by persistence-capable (a.k.a. entity) classes.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 * @see co.codewizards.cloudstore.shared.repo.local.local.AutoTrackLifecycleListener
 */
public interface AutoTrackChanged {
	/**
	 * Gets the timestamp of when this entity was last changed.
	 * @return the timestamp of when this entity was last changed. Never <code>null</code> in persistence.
	 */
	Date getChanged();
	void setChanged(Date changed);
}
