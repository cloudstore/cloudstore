package co.codewizards.cloudstore.core.chronos;

import java.util.Date;
import java.util.ServiceLoader;

public final class ChronosUtil {

	private static Chronos chronos;

	private ChronosUtil() {
	}

	protected static synchronized Chronos getChronos() {
		if (chronos == null) {
			Chronos ch = null;
			for (Chronos c : ServiceLoader.load(Chronos.class)) {
				if (ch == null || ch.getPriority() < c.getPriority() || ch.getClass().getName().compareTo(c.getClass().getName()) < 0) {
					ch = c;
				}
			}
			if (ch == null) {
				throw new IllegalStateException("No Chronos-implementation found!");
			}
			chronos = ch;
		}
		return chronos;
	}

	public static final long nowAsMillis() {
		return getChronos().nowAsMillis();
	}

	public static final Date nowAsDate() {
		return getChronos().nowAsDate();
	}
}
