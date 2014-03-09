package co.codewizards.cloudstore.core.persistence;

import java.util.Date;

/**
 * Automatically track changes by updating the {@link #getChanged() changed} property whenever
 * the object is written to the datastore.
 * <p>
 * This interface is implemented by persistence-capable (a.k.a. entity) classes. If an object
 * implementing this interface is written to the datastore (inserted or updated), the
 * {@link co.codewizards.cloudstore.core.repo.local.AutoTrackLifecycleListener AutoTrackLifecycleListener}
 * automatically invokes the {@link #setChanged(Date)} method.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 * @see co.codewizards.cloudstore.core.repo.local.AutoTrackLifecycleListener
 */
public interface AutoTrackChanged {
	/**
	 * Gets the timestamp of when this entity was last changed.
	 * @return the timestamp of when this entity was last changed. Never <code>null</code>.
	 */
	Date getChanged();
	/**
	 * Sets the timestamp of when this entity was last changed.
	 * @param changed the timestamp of when this entity was last changed. Must not be <code>null</code>.
	 */
	void setChanged(Date changed);
}
