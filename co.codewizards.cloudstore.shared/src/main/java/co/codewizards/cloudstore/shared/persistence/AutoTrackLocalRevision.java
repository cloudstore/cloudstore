package co.codewizards.cloudstore.shared.persistence;

public interface AutoTrackLocalRevision {
	/**
	 * Get the {@link LocalRepository#getRevision() local revision} of the last modification.
	 * @return the localRevision of the last modification.
	 */
	long getLocalRevision();
	void setLocalRevision(long localRevision);
}
