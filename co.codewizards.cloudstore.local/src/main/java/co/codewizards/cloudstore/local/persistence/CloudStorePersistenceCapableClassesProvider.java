package co.codewizards.cloudstore.local.persistence;

import static co.codewizards.cloudstore.core.objectfactory.ObjectFactoryUtil.*;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
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

	public static class Helper {
		public static void initPersistenceCapableClasses(final PersistenceManager pm) {
			List<CloudStorePersistenceCapableClassesProvider> providers = new LinkedList<CloudStorePersistenceCapableClassesProvider>();
			final ServiceLoader<CloudStorePersistenceCapableClassesProvider> sl = ServiceLoader.load(CloudStorePersistenceCapableClassesProvider.class);
			for (final CloudStorePersistenceCapableClassesProvider provider : sl) {
				providers.add(provider);
			}

			Collections.sort(providers, new Comparator<CloudStorePersistenceCapableClassesProvider>() {
				@Override
				public int compare(CloudStorePersistenceCapableClassesProvider o1,
						CloudStorePersistenceCapableClassesProvider o2) {
					int res = Integer.compare(o1.getOrderHint(), o2.getOrderHint());
					if (res == 0)
						res = o1.getClass().getName().compareTo(o2.getClass().getName());

					return res;
				}
			});

			for (final CloudStorePersistenceCapableClassesProvider provider : providers) {
				final Class<?>[] classes = provider.getPersistenceCapableClasses();
				if (classes != null) {
					for (Class<?> clazz : classes) {
						pm.getExtent(clazz);

						final Class<?> c = getExtendingClass(clazz);
						pm.getExtent(c);
					}
				}
			}
		}
	}
}
