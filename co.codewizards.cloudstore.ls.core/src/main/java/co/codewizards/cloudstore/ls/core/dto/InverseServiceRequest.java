package co.codewizards.cloudstore.ls.core.dto;

import java.io.Serializable;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.core.io.TimeoutException;

public interface InverseServiceRequest extends Serializable {

	Uid getRequestId();

	/**
	 * Indicates whether encountering a timeout causes the entire {@code InverseInvoker} to be marked dead.
	 * <p>
	 * If this is <code>true</code>, the first {@link TimeoutException} thrown by
	 * {@code InverseInvoker.performInverseServiceRequest(InverseServiceRequest)} causes the
	 * {@code InverseInvoker} to be marked {@code diedOfTimeout} and all future requests will fail
	 * immediately without waiting for the timeout again.
	 * <p>
	 * An implementation of {@code InverseServiceRequest} should return <code>false</code>, if it cannot
	 * guarantee that handling this request will never exceed the timeout. If, however, it knows for sure
	 * (really 100%!) that the timeout means the communication partner is dead (JVM crashed, shut down or whatever),
	 * it should return <code>true</code> to prevent unnecessary waiting.
	 * @return whether encountering a timeout once means to render the {@code InverseInvoker} out of order.
	 */
	boolean isTimeoutDeadly();

}
