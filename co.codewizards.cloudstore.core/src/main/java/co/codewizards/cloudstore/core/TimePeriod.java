package co.codewizards.cloudstore.core;

import static co.codewizards.cloudstore.core.util.StringUtil.*;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

public class TimePeriod {
	private final long millis;

	private static final String delim = " \t\u202F\u2009\r\n";
	private static final Set<Character> delimChars = new HashSet<Character>(delim.length());
	static {
		for (char c : delim.toCharArray())
			delimChars.add(c);
	}

	public TimePeriod(String string) throws ParseException {
		final Map<TimeUnit, Long> timeUnitMap = new HashMap<>();
		final StringTokenizer st = new StringTokenizer(string, delim, true);
		int offset = 0;
		long value = Long.MIN_VALUE;
		String timeUnitString = null;
		while (st.hasMoreTokens()) {
			final String tok = st.nextToken();
			if (! isDelim(tok)) {
				if (value == Long.MIN_VALUE) {
					// tok might be a combination of a number and a unit, if no delimiter is placed inbetween => try to split!
					final String[] numberAndTimeUnit = splitNumberAndTimeUnit(tok);
					try {
						value = Long.parseLong(numberAndTimeUnit[0]);
					} catch (NumberFormatException x) {
						throw new ParseException(
								String.format("The text '%s' at position %d (0-based) of the input '%s' is not a valid integer!",
										numberAndTimeUnit[0], offset, string),
								offset);
					}

					timeUnitString = numberAndTimeUnit[1];
				}
				else
					timeUnitString = tok;

				if (! isEmpty(timeUnitString)) {
					final TimeUnit timeUnit;
					try {
						timeUnit = TimeUnit.valueOf(timeUnitString);
					} catch (Exception x) {
						throw new ParseException(
								String.format("The text '%s' at position %d (0-based) of the input '%s' is not a valid time unit!",
										timeUnitString, offset, string),
								offset);
					}
					timeUnitMap.put(timeUnit, value);
					value = Long.MIN_VALUE;
					timeUnitString = null;
				}
			}
			offset += tok.length();
		}

		if (value != Long.MIN_VALUE)
			throw new ParseException(String.format("The input '%s' is missing a time unit at its end!", string), offset);

		long millis = 0;
		for (Map.Entry<TimeUnit, Long> me : timeUnitMap.entrySet())
			millis += me.getKey().toMillis(me.getValue());

		this.millis = millis;
	}

	private static boolean isDelim(final String token) {
		if (token == null || token.isEmpty())
			return true;

		for (final char c : token.toCharArray()) {
			if (! delimChars.contains(c))
				return false;
		}
		return true;
	}

	private static String[] splitNumberAndTimeUnit(final String token) {
		if (token == null || token.isEmpty())
			return new String[] { token, null };

		int index = 0;
		while (Character.isDigit(token.charAt(index))) {
			if (++index >= token.length())
				return new String[] { token, null };
		}
		return new String[] { token.substring(0, index), token.substring(index) };
	}

	public TimePeriod(long millis) {
		this.millis = millis;
	}

	@Override
	public String toString() {
		return toString(TimeUnit.getUniqueTimeUnitsOrderedByLengthDesc());
	}

	public String toString(final TimeUnit ... timeUnits) {
		return toString(timeUnits == null ? null : new HashSet<>(Arrays.asList(timeUnits)));
	}

	public String toString(final Collection<TimeUnit> timeUnits) {
		final StringBuilder sb = new StringBuilder();
		final Map<TimeUnit, Long> timeUnitMap = toTimeUnitMap(timeUnits);
		for (Map.Entry<TimeUnit, Long> me : timeUnitMap.entrySet()) {
			if (sb.length() > 0)
				sb.append(' ');

			sb.append(me.getValue()).append('\u202F').append(me.getKey()); // thin-space-separated
		}
		return sb.toString();
	}

	/**
	 * Gets the value of this time period in milliseconds.
	 * @return the value of this time period in milliseconds.
	 */
	public long toMillis() {
		return millis;
	}

	public Map<TimeUnit, Long> toTimeUnitMap() {
		return toTimeUnitMap((Collection<TimeUnit>) null);
	}

	public Map<TimeUnit, Long> toTimeUnitMap(final TimeUnit ... timeUnits) {
		return toTimeUnitMap(timeUnits == null ? null : new HashSet<>(Arrays.asList(timeUnits)));
	}

	public Map<TimeUnit, Long> toTimeUnitMap(Collection<TimeUnit> timeUnits) {
		if (timeUnits == null)
			timeUnits = TimeUnit.getUniqueTimeUnitsOrderedByLengthAsc();

		final Map<TimeUnit, Long> result = new LinkedHashMap<>();
		long remaining = millis;
		for (final TimeUnit timeUnit : timeUnits) {
			final long v = remaining / timeUnit.toMillis();
			remaining -= v * timeUnit.toMillis();

			if (v != 0)
				result.put(timeUnit, v);
		}

		if (remaining != 0)
			result.put(TimeUnit.ms, remaining);

		return result;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (millis ^ (millis >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;

		final TimePeriod other = (TimePeriod) obj;
		return this.millis == other.millis;
	}

}
