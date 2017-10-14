package co.codewizards.cloudstore.core.util;

import static org.assertj.core.api.Assertions.*;

import java.util.Calendar;

import org.junit.Test;

public class ISO8601Test {

	@Test
	public void parse() {
		Calendar cal = ISO8601.parse("2017-03-27T23:57:42.987Z");
		assertThat(cal).isNotNull();
		assertThat(cal.get(Calendar.YEAR)).isEqualTo(2017);
	}

}
