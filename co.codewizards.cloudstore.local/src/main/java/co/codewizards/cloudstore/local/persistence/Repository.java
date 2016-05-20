package co.codewizards.cloudstore.local.persistence;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.util.UUID;

import javax.jdo.annotations.Discriminator;
import javax.jdo.annotations.DiscriminatorStrategy;
import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.Unique;
import javax.jdo.listener.StoreCallback;

/**
 * A {@code Repository} represents a repository inside the database.
 * <p>
 * Every repository consists of a directory including all its sub-directories and files together with one
 * meta-data-directory containing a database. Inside this database, the local repository itself is represented
 * by a {@link LocalRepository} instance.
 * <p>
 * The local repository can be connected as a client to zero or more remote repositories. For each such remote
 * repository there is one {@link RemoteRepository} instance in the database.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
@PersistenceCapable
@Discriminator(strategy=DiscriminatorStrategy.VALUE_MAP)
@Unique(name="Repository_repositoryId", members="repositoryId")
public abstract class Repository extends Entity implements StoreCallback
{
	@Persistent(nullValue=NullValue.EXCEPTION)
	private String repositoryId;

	private long revision;

	@Persistent(nullValue=NullValue.EXCEPTION)
	private byte[] publicKey;

	public Repository() { }

	protected Repository(final UUID repositoryId) {
		// We do not create the repositoryId here anymore, because creating it lazily avoids unnecessary
		// creations when the JDO runtime instantiates objects (and reads their values from the DB anyway).
		this.repositoryId = repositoryId == null ? null : repositoryId.toString();
	}

	public UUID getRepositoryId() {
		if (repositoryId == null)
			repositoryId = createRepositoryId().toString();

		return UUID.fromString(repositoryId);
	}

	protected UUID createRepositoryId() {
		return UUID.randomUUID();
	}

	public long getRevision() {
		return revision;
	}
	public void setRevision(final long revision) {
		if (! equal(this.revision, revision))
			this.revision = revision;
	}

	public byte[] getPublicKey() {
		return publicKey;
	}

	public void setPublicKey(final byte[] publicKey) {
		if (! equal(this.publicKey, publicKey))
			this.publicKey = publicKey;
	}

	@Override
	public void jdoPreStore() {
		getRepositoryId();
	}
}