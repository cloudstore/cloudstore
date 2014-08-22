package co.codewizards.cloudstore.core.oio;

import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.oio.api.FileFactoryService;
import co.codewizards.cloudstore.oio.api.OioService;


/**
 *
 * @author Sebastian Schefczyk
 */
public class OioProvider {

	private static final Logger LOGGER = LoggerFactory.getLogger(OioProvider.class);

	private final FileFactoryService fileFactoryService;

	private static class OioProviderHolder {
		public static final OioProvider instance = new OioProvider();
	}

	private OioProvider() {
		this.fileFactoryService = getPrioritizedService(FileFactoryService.class);
		LOGGER.info("Preferred implementation '{}' for fileFactoryService", this.fileFactoryService.getClass().getSimpleName());
	}

	private <N extends OioService> N getPrioritizedService(final Class<N> n) {
		final ServiceLoader<N> serviceLoader = ServiceLoader.load(n, ClassLoader.getSystemClassLoader());
		N highPrio = null;

		for (final N i : serviceLoader) {
			try {
				if (highPrio == null)
					highPrio = i;
				else if (highPrio.getPriority() < i.getPriority())
					highPrio = i;
			} catch (final java.util.ServiceConfigurationError e) {
				// this should be caused by packaging for different distros/OS;
				LOGGER.info(e.getMessage());
			}
		}

		if (highPrio == null)
			throw new IllegalStateException("Could not get one implementation for class: " + n.getClass().getSimpleName());

		return highPrio;
	}

	public static OioProvider getInstance() {
		return OioProviderHolder.instance;
	}

	public FileFactoryService getFileFactory() {
		return fileFactoryService;
	}

	/** Checks for java.nio.file.Files at the current class loader. */
	private static boolean isJavaNioAvailable() {
		try {
			Class.forName("java.nio.file.Files");
			return true;
		} catch (final Exception e) {
			return false;
		}
	}

}
