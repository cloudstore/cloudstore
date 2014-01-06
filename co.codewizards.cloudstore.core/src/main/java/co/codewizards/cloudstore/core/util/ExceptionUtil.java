package co.codewizards.cloudstore.core.util;

import static co.codewizards.cloudstore.core.util.Util.*;

public final class ExceptionUtil {

	private ExceptionUtil() { }

	public static <T extends Throwable> T getCause(Throwable throwable, Class<T> searchClass) {
		assertNotNull("throwable", throwable);
		assertNotNull("searchClass", searchClass);

		Throwable cause = throwable;
		while (cause != null) {
			if (searchClass.isInstance(cause)) {
				return searchClass.cast(cause);
			}
			cause = cause.getCause();
		}
		return null;
	}
}
