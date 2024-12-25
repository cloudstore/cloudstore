package co.codewizards.cloudstore.core.chronos;

import java.util.Date;

public interface Chronos {

	/**
	 * Gets the priority of this {@code Chronos} implementation. The {@link ChronosUtil} chooses
	 * the {@code Chronos} with the highest priority (the greatest number).
	 * @return the priority of this {@code Chronos} implementation.
	 */
	int getPriority();

	long nowAsMillis();

	Date nowAsDate();
}
