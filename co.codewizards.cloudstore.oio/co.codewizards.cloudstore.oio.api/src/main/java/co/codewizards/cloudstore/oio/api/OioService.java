package co.codewizards.cloudstore.oio.api;

import java.util.ServiceLoader;

/**
 * Interface for fetching and scoring wrapped services for Oio.
 * <p>
 * "Oio" is our <i>own I/O</i> implementation/wrapper for java.io and java.nio.
 * Also "o" is the follow-up of "n" (from 'nio') in the alphabet.
 *
 * @author Sebastian Schefczyk
 */
public interface OioService {

	/** Priority of use. Used after getting several possible Services fetched by
	 * {@link ServiceLoader#load}. The one with the highest priority wins. */
	int getPriority();

}
