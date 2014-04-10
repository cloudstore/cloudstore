package co.codewizards.cloudstore.local.persistence;

import java.util.UUID;

import javax.jdo.annotations.Discriminator;
import javax.jdo.annotations.DiscriminatorStrategy;
import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.Unique;

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
public abstract class Repository extends Entity
{
	@Persistent(nullValue=NullValue.EXCEPTION)
	private String repositoryId;

	private long revision;

	@Persistent(nullValue=NullValue.EXCEPTION)
	private byte[] publicKey;

	public Repository() {
		this(null);
	}

	protected Repository(UUID repositoryId) {
		this.repositoryId = repositoryId == null ? UUID.randomUUID().toString() : repositoryId.toString();
	}

	public UUID getRepositoryId() {
		return UUID.fromString(repositoryId);
	}

	public long getRevision() {
		return revision;
	}
	public void setRevision(long revision) {
		this.revision = revision;
	}

	public byte[] getPublicKey() {
		return publicKey;
	}

	public void setPublicKey(byte[] publicKey) {
		this.publicKey = publicKey;
	}
}