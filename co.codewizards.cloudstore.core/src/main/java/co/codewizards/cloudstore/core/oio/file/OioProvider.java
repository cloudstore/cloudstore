package co.codewizards.cloudstore.core.oio.file;

import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * Is the package 'java.nio' available?
 *
 * Operation dependent stuff: are symlinks available in this directory? For example: An USB stick with fat32, even under Linux.
 *
 * @author Sebastian Schefczyk
 */
public class OioProvider {

	private static final Logger LOGGER = LoggerFactory.getLogger(OioProvider.class);

	private final File file;

	private static class OioProviderHolder {
		public static final OioProvider instance = new OioProvider();
	}

	protected OioProvider() {
		this.file = getPrioritizedService(File.class);
		LOGGER.info("Preferred implementation '{}' for file", this.file.getClass().getSimpleName());
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
			throw new IllegalStateException("Could not get one implementation for class: " + n.getClass());

		return highPrio;
	}

	public static OioProvider getInstance() {
		return OioProviderHolder.instance;
	}

	/** Replacement for java.nio.file.Files */
	public File file() {
		return file;
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
