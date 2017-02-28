package co.codewizards.cloudstore.core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DebugUtil {

	private static final Logger logger = LoggerFactory.getLogger(DebugUtil.class);

	private static final long KIB = 1024L;
	private static final long MIB = KIB * 1024;
	private static final long GIB = MIB * 1024;
	private static final long TIB = GIB * 1024;

	private DebugUtil() {
	}

	public static void logMemoryStats(Logger logger) {
		if (logger == null)
			logger = DebugUtil.logger;

		if (! logger.isInfoEnabled())
			return;

		final Runtime runtime = Runtime.getRuntime();

		runtime.gc();

		// max: limit of maximum allocatable memory allowed to the VM. Likely specified by -Xmx...
		final long max = runtime.maxMemory();

		// allocated: memory currently allocated by the VM (requested from and granted by the OS). Might be less than 'max'.
		final long allocated = runtime.totalMemory();

		// used: memory in use by Java objects (hence we invoke gc() above, otherwise this doesn't say anything useful).
		final long used = allocated - runtime.freeMemory();

		// available: how much this JVM can still use for future objects -- with or without the need to allocate more from the OS.
		final long available = max - used;

		logger.info("logMemoryStats: max={}, allocated={}, used={}, available={}",
				getHumanReadableSize(max),
				getHumanReadableSize(allocated),
				getHumanReadableSize(used),
				getHumanReadableSize(available));
	}

	private static String getHumanReadableSize(final long size) {
		if (size >= TIB)
			return String.format("%.1f TiB", (double) size / TIB);

		if (size >= GIB)
			return String.format("%.1f GiB", (double) size / GIB);

		if (size >= MIB)
			return String.format("%.1f MiB", (double) size / MIB);

		if (size >= KIB)
			return String.format("%.1f KiB", (double) size / KIB);

		return String.format("%d B", size);
	}
}
