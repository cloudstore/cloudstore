package co.codewizards.cloudstore.core.dto;

import static org.assertj.core.api.Assertions.*;

import java.util.Date;
import java.util.Random;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DateTimeTest {
	private final Logger logger = LoggerFactory.getLogger(DateTimeTest.class);
	@Test
	public void convertToStringAndBackPositiveOnly() {
		Random random = new Random();
		DateTime dateTime = new DateTime(new Date(Math.abs(random.nextLong())));
		long dateTimeLong = dateTime.getMillis();
		logger.info("dateTimeLong: {}", dateTimeLong);
		String dateTimeString = dateTime.toString();
		logger.info("dateTimeString: {}", dateTimeString);
		DateTime dateTime2 = new DateTime(dateTimeString);
		long dateTimeLong2 = dateTime2.getMillis();
		logger.info("dateTimeLong2: {}", dateTimeLong2);
		assertThat(dateTime2).isEqualTo(dateTime);
	}

	@Test
	public void convertToStringAndBack() {
		Random random = new Random();
		DateTime dateTime = new DateTime(new Date(random.nextLong()));
		long dateTimeLong = dateTime.getMillis();
		logger.info("dateTimeLong: {}", dateTimeLong);
		String dateTimeString = dateTime.toString();
		logger.info("dateTimeString: {}", dateTimeString);
		DateTime dateTime2 = new DateTime(dateTimeString);
		long dateTimeLong2 = dateTime2.getMillis();
		logger.info("dateTimeLong2: {}", dateTimeLong2);
		assertThat(dateTime2).isEqualTo(dateTime);
	}
}
