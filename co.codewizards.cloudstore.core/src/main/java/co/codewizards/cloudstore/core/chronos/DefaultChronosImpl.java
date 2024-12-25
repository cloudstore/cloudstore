package co.codewizards.cloudstore.core.chronos;

import java.util.Calendar;

public class DefaultChronosImpl extends AbstractChronos {

	public static final String ENV_TEST_YEAR = "TEST_YEAR";

	public static Integer getTestYear() {
		String s = System.getenv(ENV_TEST_YEAR);
		if (s == null) {
			return null;
		}
		s = s.trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return Integer.valueOf(s);
		} catch (NumberFormatException x) {
			NumberFormatException y = new NumberFormatException(String.format("Env-var '%s' contains illegal value '%s'!", ENV_TEST_YEAR, s));
			y.initCause(x);
			throw y;
		}
	}

	@Override
	public int getPriority() {
		return 0;
	}

	@Override
	public long nowAsMillis() {
		Integer testYear = getTestYear();
		if (testYear != null) {
			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(System.currentTimeMillis());
			cal.set(Calendar.YEAR, testYear);
			return cal.getTimeInMillis();
		}
		return System.currentTimeMillis();
	}
}