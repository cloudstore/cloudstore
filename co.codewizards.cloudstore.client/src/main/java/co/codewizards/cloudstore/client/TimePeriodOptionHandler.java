package co.codewizards.cloudstore.client;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.OneArgumentOptionHandler;
import org.kohsuke.args4j.spi.Setter;

import co.codewizards.cloudstore.core.TimeUnit;

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
	public TimePeriodOptionHandler(CmdLineParser parser, OptionDef option, Setter<Long> setter)
	{
		super(parser, option, setter);
	}

	@Override
	protected Long parse(String argument) throws NumberFormatException, CmdLineException
	{
		TimeUnit timeUnit = null;
		for (TimeUnit u : TimeUnit.values()) {
			if (argument.endsWith(u.name()) && (timeUnit == null || timeUnit.name().length() < u.name().length()))
				timeUnit = u;
		}

		if (timeUnit == null)
			throw new CmdLineException(owner, "Argument '" + argument + "' does not end with one of the following unit-suffixes: " + TimeUnit.getAllUnitsWithDisplayName());

		String numberVal = argument.substring(0, argument.length() - timeUnit.name().length()).trim();
		long valueMSec = Long.parseLong(numberVal);
		return timeUnit.toMillis(valueMSec);
	}

}
