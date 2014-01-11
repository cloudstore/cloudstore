package co.codewizards.cloudstore.rest.server.auth;

import static org.assertj.core.api.Assertions.*;

import org.junit.Test;

public class PasswordUtilTest {

	@Test
	public void assertValidMinAndMaxLength() {
		int lengthExpected = 30;
		int minLengthActual = Integer.MAX_VALUE;
		int maxLengthActual = 0;
		for (int i = 0; i < 10000; ++i) {
			char[] password = PasswordUtil.createRandomPassword(lengthExpected);
			minLengthActual = Math.min(minLengthActual, password.length);
			maxLengthActual = Math.max(maxLengthActual, password.length);
		}
		assertThat(minLengthActual).isEqualTo(lengthExpected);
		assertThat(maxLengthActual).isEqualTo(lengthExpected);
	}

}
