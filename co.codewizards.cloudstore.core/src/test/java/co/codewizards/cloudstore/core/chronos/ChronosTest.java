package co.codewizards.cloudstore.core.chronos;

import static org.assertj.core.api.Assertions.*;
import static org.junit.Assume.*;

import java.util.Date;

import org.junit.Test;

import co.codewizards.cloudstore.core.dto.DateTime;

public class ChronosTest {

	private static final Integer YEAR_2014 = 2014;

	@Test
	public void assert_ENV_TEST_YEAR() {
		Integer testYear = DefaultChronosImpl.getTestYear();
		assertThat(testYear).isNotNull();
		assertThat(testYear).isEqualTo(YEAR_2014);
	}

	@Test
	public void nowAsMillisTest() {
		assumeTrue(YEAR_2014.equals(DefaultChronosImpl.getTestYear()));
		long now = ChronosUtil.nowAsMillis();
		assertThat(now).isGreaterThanOrEqualTo(new DateTime("2014-01-01T00:00:00.000Z").getMillis());
		assertThat(now).isLessThanOrEqualTo(new DateTime("2014-12-31T23:59:59.999Z").getMillis());
	}

	@Test
	public void nowAsDateTest() {
		assumeTrue(YEAR_2014.equals(DefaultChronosImpl.getTestYear()));
		Date now = ChronosUtil.nowAsDate();
		assertThat(now).isAfterOrEqualsTo(new DateTime("2014-01-01T00:00:00.000Z").toDate());
		assertThat(now).isBeforeOrEqualsTo(new DateTime("2014-12-31T23:59:59.999Z").toDate());
	}
}
