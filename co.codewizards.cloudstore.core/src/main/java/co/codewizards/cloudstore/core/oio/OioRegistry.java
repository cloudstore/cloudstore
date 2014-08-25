package co.codewizards.cloudstore.core.oio;

import java.util.Iterator;
import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * @author Sebastian Schefczyk
 */
public class OioRegistry {

	private static final Logger LOGGER = LoggerFactory.getLogger(OioRegistry.class);

	private final FileFactory fileFactory;

	private static class OioProviderHolder {
		public static final OioRegistry instance = new OioRegistry();
	}

	private OioRegistry() {
		this.fileFactory = getPrioritizedService(FileFactory.class);
		LOGGER.info("Preferred implementation '{}' for fileFactory", this.fileFactory.getClass().getSimpleName());
	}

	private <N extends FileFactory> N getPrioritizedService(final Class<N> n) {
		final Iterator<N> it = ServiceLoader.load(n, n.getClassLoader()).iterator();
		N highPrio = null;

		while (it.hasNext()) {
			final N i = it.next();
			if (highPrio == null)
				highPrio = i;
			else if (highPrio.getPriority() < i.getPriority())
				highPrio = i;
		}

		if (highPrio == null)
			throw new IllegalStateException("Could not get one implementation for class: " + n.getClass().getSimpleName());

		return highPrio;
	}

	public static OioRegistry getInstance() {
		return OioProviderHolder.instance;
	}

	public FileFactory getFileFactory() {
		return fileFactory;
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
