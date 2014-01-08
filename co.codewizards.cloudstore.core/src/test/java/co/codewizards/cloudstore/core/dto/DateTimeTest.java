package co.codewizards.cloudstore.core.dto;

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
	private Random random = new Random();

	@Test
	public void convertToStringAndBack() {
		DateTime dateTime = new DateTime(nextValidRandomDate());
		long dateTimeLong = dateTime.getMillis();
		logger.info("dateTimeLong: {}", dateTimeLong);
		String dateTimeString = dateTime.toString();
		logger.info("dateTimeString: {}", dateTimeString);
		DateTime dateTime2 = new DateTime(dateTimeString);
		long dateTimeLong2 = dateTime2.getMillis();
		logger.info("dateTimeLong2: {}", dateTimeLong2);
		assertThat(dateTime2).isEqualTo(dateTime);
	}

	/**
	 * Return the next random date that is valid in ISO8601.
	 * ISO8601 only accepts 4-digit-years, i.e. from -9999 to 9999.
	 * @return a random date that can be encoded according to ISO8601.
	 */
	private Date nextValidRandomDate() {
		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.UK);
		calendar.setTimeInMillis(random.nextLong());
		int year = calendar.get(Calendar.YEAR);
		year = year % 9999;
		calendar.set(Calendar.YEAR, year);
		return calendar.getTime();
	}

}
