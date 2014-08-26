package co.codewizards.cloudstore.core.oio;

/**
 * Substitute for java.nio.channels.OverlappingFileLockException.
 */
public class OverlappingFileLockException extends IllegalStateException {

	private static final long serialVersionUID = 6335515229586129038L;

	public OverlappingFileLockException() { }

}
