package co.codewizards.cloudstore.core.util;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

public final class ExceptionUtil {

	private ExceptionUtil() { }

	public static <T extends Throwable> T getCause(final Throwable throwable, final Class<T> searchClass) {
		assertNotNull(throwable, "throwable");
		assertNotNull(searchClass, "searchClass");

		Throwable cause = throwable;
		while (cause != null) {
			if (searchClass.isInstance(cause)) {
				return searchClass.cast(cause);
			}
			cause = cause.getCause();
		}
		return null;
	}

	public static RuntimeException throwThrowableAsRuntimeExceptionIfNeeded(final Throwable throwable) {
		assertNotNull(throwable, "throwable");
		if (throwable instanceof Error)
			throw (Error) throwable;

		if (throwable instanceof RuntimeException)
			throw (RuntimeException) throwable;

		throw new RuntimeException(throwable);
	}
}
