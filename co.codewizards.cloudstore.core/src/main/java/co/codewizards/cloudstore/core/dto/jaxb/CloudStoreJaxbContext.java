package co.codewizards.cloudstore.core.dto.jaxb;

import java.util.HashSet;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

public class CloudStoreJaxbContext {

	private static class JaxbContextHolder {
		private static final JAXBContext jaxbContext;
		static {
			final Set<Class<?>> collectedClassesToBeBound = new HashSet<Class<?>>();
			final ServiceLoader<CloudStoreJaxbContextProvider> serviceLoader = ServiceLoader.load(CloudStoreJaxbContextProvider.class);
			for (final Iterator<CloudStoreJaxbContextProvider> it = serviceLoader.iterator(); it.hasNext(); ) {
				final CloudStoreJaxbContextProvider provider = it.next();
				final Class<?>[] classesToBeBound = provider.getClassesToBeBound();
				if (classesToBeBound != null) {
					for (final Class<?> clazz : classesToBeBound)
						collectedClassesToBeBound.add(clazz);
				}
			}
			try {
				final Class<?>[] ca = collectedClassesToBeBound.toArray(new Class[collectedClassesToBeBound.size()]);
				jaxbContext = JAXBContext.newInstance(ca);
			} catch (JAXBException x) {
				throw new RuntimeException(x);
			}
		}
	}

	public static JAXBContext getJaxbContext() {
		return JaxbContextHolder.jaxbContext;
	}
}
