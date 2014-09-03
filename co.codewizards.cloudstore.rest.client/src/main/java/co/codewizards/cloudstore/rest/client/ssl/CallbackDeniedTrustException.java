package co.codewizards.cloudstore.rest.client.ssl;

/**
 * Exception indicating that {@link DynamicX509TrustManagerCallback} was consulted and explicitly denied
 * trust.
 * <p>
 * This exception is used to suppress multiple retries which would trigger the callback to be consulted
 * multiple times.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public class CallbackDeniedTrustException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public CallbackDeniedTrustException(final Throwable cause) {
		super(cause);
	}

}
