package co.codewizards.cloudstore.core.persistence;

/**
 * Automatically track changes by updating the {@link #getLocalRevision() localRevision} property whenever
 * the object is written to the datastore.
 * <p>
 * This interface is implemented by persistence-capable (a.k.a. entity) classes.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 * @see co.codewizards.cloudstore.core.repo.local.local.AutoTrackLifecycleListener
 */
public interface AutoTrackLocalRevision {
	/**
	 * Get the {@link LocalRepository#getRevision() local revision} of the last modification
	 * of this entity.
	 * @return the localRevision of the last modification.
	 */
	long getLocalRevision();
	void setLocalRevision(long localRevision);
}
