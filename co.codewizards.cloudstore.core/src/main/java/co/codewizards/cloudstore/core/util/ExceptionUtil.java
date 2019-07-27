package co.codewizards.cloudstore.core.util;

import static java.util.Objects.*;

public final class ExceptionUtil {

	private ExceptionUtil() { }

	public static <T extends Throwable> T getCause(final Throwable throwable, final Class<T> searchClass) {
		requireNonNull(throwable, "throwable");
		requireNonNull(searchClass, "searchClass");

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
		requireNonNull(throwable, "throwable");
		if (throwable instanceof Error)
			throw (Error) throwable;

		if (throwable instanceof RuntimeException)
			throw (RuntimeException) throwable;

		throw new RuntimeException(throwable);
	}
}
