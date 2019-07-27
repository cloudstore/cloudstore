package co.codewizards.cloudstore.local.persistence;

import static co.codewizards.cloudstore.core.util.Util.*;
import static java.util.Objects.*;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.annotations.Discriminator;
import javax.jdo.annotations.DiscriminatorStrategy;
import javax.jdo.annotations.Inheritance;
import javax.jdo.annotations.InheritanceStrategy;
import javax.jdo.annotations.Join;
import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;

/**
 * A {@code LocalRepository} represents the local repository inside the repository's database.
 * <p>
 * There is exactly one {@code LocalRepository} instance in the database.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
@PersistenceCapable
@Inheritance(strategy=InheritanceStrategy.SUPERCLASS_TABLE)
@Discriminator(strategy=DiscriminatorStrategy.VALUE_MAP, value="LocalRepository")
public class LocalRepository extends Repository {

	@Persistent(nullValue=NullValue.EXCEPTION)
	private Directory root;

	@Persistent(nullValue=NullValue.EXCEPTION)
	private byte[] privateKey;

	@Join
	private Set<String> aliases;

	public LocalRepository() { }

	public LocalRepository(UUID repositoryId) {
		super(repositoryId);
	}

	/**
	 * Get the root directory of this repository.
	 * @return the root directory of this repository. Never <code>null</code> in persistence.
	 */
	public Directory getRoot() {
		return root;
	}
	public void setRoot(final Directory root) {
		if (! equal(this.root, root))
			this.root = root;
	}

	public byte[] getPrivateKey() {
		return privateKey;
	}

	public void setPrivateKey(final byte[] privateKey) {
		if (! equal(this.privateKey, privateKey))
			this.privateKey = privateKey;
	}

	public Set<String> getAliases() {
		if (aliases == null)
			aliases = new HashSet<>();

		return aliases;
	}

	@Override
	public void jdoPreStore() {
		super.jdoPreStore();
		final PersistenceManager pm = requireNonNull(JDOHelper.getPersistenceManager(this), "JDOHelper.getPersistenceManager(this)");
		final Iterator<LocalRepository> iterator = pm.getExtent(LocalRepository.class).iterator();
		if (iterator.hasNext()) {
			final LocalRepository persistentInstance = iterator.next();
			if (iterator.hasNext())
				throw new IllegalStateException("There are multiple LocalRepository entities in the database.");

			if (persistentInstance != null && ! persistentInstance.equals(this))
				throw new IllegalStateException("Cannot persist a 2nd LocalRepository!");
		}
	}
}