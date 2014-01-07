package co.codewizards.cloudstore.client;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.OneArgumentOptionHandler;
import org.kohsuke.args4j.spi.Setter;

/**
 * <p>
 * Option handler implementation to interprete a time period (e.g. "5 minutes".
 * </p>
 * <p>
 * The time period is specified in the command line by writing a number
 * directly followed (no space!) by a unit. For example 5 minutes could be
 * written as "5min" or "300s" (300 seconds are 5 minutes).
 * </p>
 * <p>
 * This handler can be chosen for every <code>long</code> property using
 * the {@link Option} annotation like this:
 * </p>
 * <pre>
 * &#64;Option(name="-myArg", handler=TimePeriodOptionHandler.class)
 * private long myArg;
 * </pre>
 * <p>
 * The <code>long</code> property will be set to the milliseconds value.
 * For example, if the command line user passes "5min", the <code>long</code> value
 * will be 300000 (5 min * 60 s * 1000 ms).
 * </p>
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
public class TimePeriodOptionHandler extends OneArgumentOptionHandler<Long>
{
	/**
	 * Units based on <a target="_blank" href="http://en.wikipedia.org/wiki/ISO_31-1">ISO 31-1</a> (where it exists).
	 *
	 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
	 */
	public static enum Unit {
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

		private String displayName;
		private long msec;

		private Unit(String displayName, long msec)
		{
			this.displayName = displayName;
			this.msec = msec;
		}

		public long toMSec(long value)
		{
			return value * msec;
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

			for (Unit u : values()) {
				if (sb.length() > 0)
					sb.append(separator);

				sb.append(String.format(unitFormat, u.name(), u.getDisplayName()));
			}

			return sb.toString();
		}
	}

	public TimePeriodOptionHandler(CmdLineParser parser, OptionDef option, Setter<Long> setter)
	{
		super(parser, option, setter);
	}

	@Override
	protected Long parse(String argument) throws NumberFormatException, CmdLineException
	{
		Unit unit = null;
		for (Unit u : Unit.values()) {
			if (argument.endsWith(u.name()) && (unit == null || unit.name().length() < u.name().length()))
				unit = u;
		}

		if (unit == null)
			throw new CmdLineException(owner, "Argument '" + argument + "' does not end with one of the following unit-suffixes: " + Unit.getAllUnitsWithDisplayName());

		String numberVal = argument.substring(0, argument.length() - unit.name().length()).trim();
		long valueMSec = Long.parseLong(numberVal);
		return unit.toMSec(valueMSec);
	}

}
