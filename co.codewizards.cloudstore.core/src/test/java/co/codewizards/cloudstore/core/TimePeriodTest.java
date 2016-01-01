package co.codewizards.cloudstore.core;

import static org.assertj.core.api.Assertions.*;

import java.text.ParseException;

import org.junit.Test;

public class TimePeriodTest {

	@Test
	public void parseString1() throws ParseException {
		long millis = new TimePeriod("3 h 5 min").toMillis();
		assertThat(millis).isEqualTo(3L * 3600 * 1000 + 5 * 60 * 1000);
	}

	@Test
	public void parseString2() throws ParseException {
		long millis = new TimePeriod("3h 5min").toMillis();
		assertThat(millis).isEqualTo(3L * 3600 * 1000 + 5 * 60 * 1000);
	}

	@Test
	public void parseString3() throws ParseException {
		long millis = new TimePeriod("5 a   3h 5  min").toMillis();
		assertThat(millis).isEqualTo(5L * 365 * 24 * 3600 * 1000 + 3 * 3600 * 1000 + 5 * 60 * 1000);
	}

	@Test
	public void toString1() throws ParseException {
		TimePeriod timePeriod = new TimePeriod(5L * 365 * 24 * 3600 * 1000 + 3 * 3600 * 1000 + 5 * 60 * 1000);
		String string = timePeriod.toString();
		assertThat(string).isEqualTo("5\u202Fa 3\u202Fh 5\u202Fmin");
	}
}
