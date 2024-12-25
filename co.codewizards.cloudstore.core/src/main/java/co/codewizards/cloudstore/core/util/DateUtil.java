package co.codewizards.cloudstore.core.util;

import static co.codewizards.cloudstore.core.chronos.ChronosUtil.*;

import java.util.Date;

import co.codewizards.cloudstore.core.chronos.ChronosUtil;

public final class DateUtil {

	private DateUtil() {
	}

	/**
	 * Gets the timestamp of now.
	 * @return the current timestamp. Never <code>null</code>.
	 * @deprecated Replace by {@link ChronosUtil#nowAsDate()}!
	 */
	@Deprecated
	public static Date now() {
		return nowAsDate();
	}

	/**
	 * Copies the given {@code date}. If the input is <code>null</code>, the result is
	 * <code>null</code>, too. Otherwise the result is an instance of {@link Date}
	 * (even if the input is an instance of a subclass) representing the same timestamp as
	 * the input.
	 * @param date the date instance to be copied. May be <code>null</code>.
	 * @return the copied date or <code>null</code> (if the input was <code>null</code>).
	 */
	public static Date copyDate(Date date) {
		return date == null ? null : new Date(date.getTime());
	}
}
