package co.codewizards.cloudstore.local.persistence;

import javax.jdo.annotations.Discriminator;
import javax.jdo.annotations.DiscriminatorStrategy;
import javax.jdo.annotations.Index;
import javax.jdo.annotations.Indices;
import javax.jdo.annotations.Inheritance;
import javax.jdo.annotations.InheritanceStrategy;
import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.Queries;
import javax.jdo.annotations.Query;

@PersistenceCapable
@Inheritance(strategy=InheritanceStrategy.NEW_TABLE)
@Discriminator(strategy=DiscriminatorStrategy.VALUE_MAP)
@Indices({
	@Index(name="Modification_remoteRepository_localRevision", members={"remoteRepository", "localRevision"}),
})
@Queries({
	@Query(name="getModificationsAfter_remoteRepository_localRevision", value="SELECT WHERE this.remoteRepository == :remoteRepository && this.localRevision > :localRevision"),
	@Query(name="getModificationsBeforeOrEqual_remoteRepository_localRevision", value="SELECT WHERE this.remoteRepository == :remoteRepository && this.localRevision <= :localRevision")
})
public abstract class Modification extends Entity implements AutoTrackLocalRevision {

	@Persistent(nullValue=NullValue.EXCEPTION)
	private RemoteRepository remoteRepository;

	private long localRevision;

	/**
	 * Gets the remote repository to which this modification must be synced.
	 * @return the remote repository to which this modification must be synced.
	 */
	public RemoteRepository getRemoteRepository() {
		return remoteRepository;
	}

	public void setRemoteRepository(RemoteRepository remoteRepository) {
		this.remoteRepository = remoteRepository;
	}

	@Override
	public long getLocalRevision() {
		return localRevision;
	}
	@Override
	public void setLocalRevision(long revision) {
		this.localRevision = revision;
	}
}
