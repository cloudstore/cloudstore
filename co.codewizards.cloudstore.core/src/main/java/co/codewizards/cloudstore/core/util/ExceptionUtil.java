package co.codewizards.cloudstore.core.util;

public final class ExceptionUtil {

	private ExceptionUtil() { }

	public static <T extends Throwable> T getCause(Throwable throwable, Class<T> searchClass) {
		AssertUtil.assertNotNull(throwable, "throwable");
		AssertUtil.assertNotNull(searchClass, "searchClass");

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
