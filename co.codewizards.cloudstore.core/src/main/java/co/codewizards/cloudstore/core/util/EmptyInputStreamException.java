package co.codewizards.cloudstore.core.util;

/**
 * Throw it in a case of an empty input stream.
 *
 * @author Sebastian Schefczyk
 */
public  class EmptyInputStreamException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public EmptyInputStreamException(final String message) {
		super(message);
	}
}