package co.codewizards.cloudstore.core.persistence;

import javax.jdo.annotations.Discriminator;
import javax.jdo.annotations.DiscriminatorStrategy;
import javax.jdo.annotations.Inheritance;
import javax.jdo.annotations.InheritanceStrategy;
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

	public LocalRepository() { }

	/**
	 * Get the root directory of this repository.
	 * @return the root directory of this repository. Never <code>null</code> in persistence.
	 */
	public Directory getRoot() {
		return root;
	}
	public void setRoot(Directory root) {
		this.root = root;
	}

	public byte[] getPrivateKey() {
		return privateKey;
	}

	public void setPrivateKey(byte[] privateKey) {
		this.privateKey = privateKey;
	}
}