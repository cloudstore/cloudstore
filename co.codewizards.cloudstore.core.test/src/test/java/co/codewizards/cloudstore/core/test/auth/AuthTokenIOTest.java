package co.codewizards.cloudstore.core.test.auth;

import static java.lang.System.*;
import static org.assertj.core.api.Assertions.*;

import java.util.Date;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.auth.AuthToken;
import co.codewizards.cloudstore.core.auth.AuthTokenIO;
import co.codewizards.cloudstore.core.dto.DateTime;

public class AuthTokenIOTest {

	private static final Logger logger = LoggerFactory.getLogger(AuthTokenIOTest.class);

	{
		logger.debug("[{}]<init>", Integer.toHexString(identityHashCode(this)));
	}


	@Test
	public void serialiseAndDeserialise() {
		logger.debug("[{}]serialiseAndDeserialise: entered.", Integer.toHexString(identityHashCode(this)));
		final AuthToken authToken = createAuthToken();

		final AuthTokenIO io = new AuthTokenIO();
		final byte[] authTokenData = io.serialise(authToken);
		assertThat(authTokenData).isNotNull();

		final AuthToken authToken2 = io.deserialise(authTokenData);
		assertThat(authToken2).isNotNull();

		assertThat(authToken2.getExpiryDateTime()).isEqualTo(authToken.getExpiryDateTime());
		assertThat(authToken2.getPassword()).isEqualTo(authToken.getPassword());
	}

	public static AuthToken createAuthToken() {
		final AuthToken authToken = new AuthToken();
		authToken.setExpiryDateTime(new DateTime(new Date()));
		authToken.setRenewalDateTime(new DateTime(new Date()));
		authToken.setPassword("fadgfsdagasd");
		return authToken;
	}
}
