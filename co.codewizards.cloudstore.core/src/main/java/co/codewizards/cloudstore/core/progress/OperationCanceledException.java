package co.codewizards.cloudstore.core.progress;

/**
 * Exception thrown to exit a long-running method
 * after the user cancelled it. If code checking {@link ProgressMonitor#isCanceled()}
 * finds <code>true</code>, it should throw this exception.
 */
public class OperationCanceledException extends RuntimeException
{
	private static final long serialVersionUID = 1L;

	public OperationCanceledException() { }

	public OperationCanceledException(String message) {
		super(message);
	}
}
