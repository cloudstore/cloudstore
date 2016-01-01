package co.codewizards.cloudstore.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

/**
 * Units based on <a target="_blank" href="http://en.wikipedia.org/wiki/ISO_31-1">ISO 31-1</a> (where it exists).
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
public enum TimeUnit {
	/**
	 * Millisecond.
	 */
	ms("Millisecond", 1L),

	/**
	 * Second.
	 */
	s("Second", 1000L),

	/**
	 * Minute.
	 */
	min("Minute", 60L * s.msec),

	/**
	 * Hour.
	 */
	h("Hour", 60L * min.msec),

	/**
	 * Day.
	 */
	d("Day", 24L * h.msec),

	/**
	 * Year. <a target="_blank" href="http://en.wikipedia.org/wiki/Year">Abbreviation from latin "annus".</a>
	 */
	a("Year", 365L * d.msec),

	/**
	 * Year (alternative for convenience).
	 */
	y("Year", 365L * d.msec)
	;

	private final String displayName;
	private final long msec;

	private TimeUnit(String displayName, long msec)
	{
		this.displayName = displayName;
		this.msec = msec;
	}

	public long toMillis(long value)
	{
		return value * msec;
	}

	public long toMillis()
	{
		return msec;
	}

	public String getDisplayName() {
		return displayName;
	}

	public static String getAllUnitsWithDisplayName()
	{
		return getAllUnitsWithDisplayName(", ");
	}

	public static String getAllUnitsWithDisplayName(String separator)
	{
		return getAllUnitsWithDisplayName("%s (%s)", separator);
	}

	public static String getAllUnitsWithDisplayName(String unitFormat, String separator)
	{
		StringBuilder sb = new StringBuilder();

		for (TimeUnit u : values()) {
			if (sb.length() > 0)
				sb.append(separator);

			sb.append(String.format(unitFormat, u.name(), u.getDisplayName()));
		}

		return sb.toString();
	}

	public static List<TimeUnit> getUniqueTimeUnitsOrderedByLengthAsc() {
		final List<TimeUnit> result = new ArrayList<>(Arrays.asList(TimeUnit.values()));
		result.remove(TimeUnit.y); // "y" is redundant - using only "a" by default.
		return Collections.unmodifiableList(result);
	}

	public static List<TimeUnit> getUniqueTimeUnitsOrderedByLengthDesc() {
		final List<TimeUnit> asc = getUniqueTimeUnitsOrderedByLengthAsc();
		final List<TimeUnit> desc = new ArrayList<TimeUnit>(asc.size());
		for (final ListIterator<TimeUnit> it = asc.listIterator(asc.size()); it.hasPrevious();) {
			final TimeUnit timeUnit = it.previous();
			desc.add(timeUnit);
		}
		return desc;
	}
}