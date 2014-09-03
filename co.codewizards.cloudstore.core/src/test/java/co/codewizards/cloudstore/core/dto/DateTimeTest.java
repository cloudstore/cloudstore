package co.codewizards.cloudstore.core.dto;

import static java.lang.System.*;
import static org.assertj.core.api.Assertions.*;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DateTimeTest {
	private final Logger logger = LoggerFactory.getLogger(DateTimeTest.class);
	private final Random random = new Random();

	{
		logger.debug("[{}]<init>", Integer.toHexString(identityHashCode(this)));
	}

	@Test
	public void convertToStringAndBack() {
		logger.debug("[{}]convertToStringAndBack: entered.", Integer.toHexString(identityHashCode(this)));
		final DateTime dateTime = new DateTime(nextValidRandomDate());
		final long dateTimeLong = dateTime.getMillis();
		logger.info("dateTimeLong: {}", dateTimeLong);
		final String dateTimeString = dateTime.toString();
		logger.info("dateTimeString: {}", dateTimeString);
		final DateTime dateTime2 = new DateTime(dateTimeString);
		final long dateTimeLong2 = dateTime2.getMillis();
		logger.info("dateTimeLong2: {}", dateTimeLong2);
		assertThat(dateTime2).isEqualTo(dateTime);
	}

	/**
	 * Return the next random date that is valid in ISO8601.
	 * ISO8601 only accepts 4-digit-years, i.e. from -9999 to 9999.
	 * @return a random date that can be encoded according to ISO8601.
	 */
	private Date nextValidRandomDate() {
		final Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.UK);
		calendar.setTimeInMillis(random.nextLong());
		int year = calendar.get(Calendar.YEAR);
		year = year % 9999;
		calendar.set(Calendar.YEAR, year);
		return calendar.getTime();
	}

}
