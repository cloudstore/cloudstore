package co.codewizards.cloudstore.local.persistence;

import static co.codewizards.cloudstore.core.util.Util.*;

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
/**
 * @deprecated Storing one Modification per remote-repository is highly inefficient and not necessary. We should replace
 * Modification (and its subclasses) by a new class (with appropriate sub-classes) that is *not* remote-repository-dependent!
 * We could call it 'Modification2' or better simply 'Mod' (with 'DeleteMod' and 'CopyMod' etc.).
 * A 'Mod' can be deleted, if it was replicated to all remote-repositories. We can track this easily: It is the case, if
 * for all remote-repositories, the condition 'Mod.localRevision <= LastCryptoKeySyncToRemoteRepo.localRepositoryRevisionSynced' is met.
 * Note, that the ChangeSet should only contain a 'Mod', if 'Mod.localRevision > LastCryptoKeySyncToRemoteRepo.localRepositoryRevisionSynced',
 * just like it is done for RepoFiles.
 * TODO Refactor per-remote-repo 'Modification' to global 'Mod'! Keep downward-compatibility!!! Upgrading existing repos should work fine!
 */
@Deprecated
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
		if (! equal(this.remoteRepository, remoteRepository))
			this.remoteRepository = remoteRepository;
	}

	@Override
	public long getLocalRevision() {
		return localRevision;
	}
	@Override
	public void setLocalRevision(long revision) {
		if (! equal(this.localRevision, revision))
			this.localRevision = revision;
	}
}
