package co.codewizards.cloudstore.core.oio;

import java.util.ServiceLoader;

/**
 * Common interface for loading an implementation of a factory. If more than
 * one implementations are found, the one with the highest priority will be
 * selected.
 * @author Sebastian Schefczyk
 */
public interface FileService {

	/** Priority of use. Used after getting more than one services fetched by
	 * {@link ServiceLoader#load}. The one with the highest priority wins. */
	int getPriority();

}
