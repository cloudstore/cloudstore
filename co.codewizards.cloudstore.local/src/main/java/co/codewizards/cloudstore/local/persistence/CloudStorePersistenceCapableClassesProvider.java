package co.codewizards.cloudstore.local.persistence;

import java.util.ServiceLoader;

import javax.jdo.PersistenceManager;
import javax.jdo.annotations.PersistenceCapable;

/**
 * {@code CloudStorePersistenceCapableClassesProvider} implementations make persistence-capable classes known
 * to the {@code LocalRepoManagerImpl}'s {@link PersistenceManager}.
 * <p>
 * Implementation classes are registered using the {@link ServiceLoader}.
 * <p>
 * <b>Important:</b> Implementors should subclass {@link AbstractCloudStorePersistenceCapableClassesProvider} instead
 * of directly implementing this interface!
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public interface CloudStorePersistenceCapableClassesProvider {

	/**
	 * Provider-instances are sorted by {@code orderHint} (ascending) and fully qualified class-name
	 * (in case the hint of 2 implementations is equal).
	 * @return the order used to sort provider-instances.
	 */
	int getOrderHint();

	/**
	 * Gets the {@linkplain PersistenceCapable persistence-capable} classes.
	 * <p>
	 * The classes returned here are combined with all other providers' results.
	 * @return the {@linkplain PersistenceCapable persistence-capable} classes. May be <code>null</code>,
	 * which is equivalent to an empty array.
	 */
	Class<?>[] getPersistenceCapableClasses();
}
