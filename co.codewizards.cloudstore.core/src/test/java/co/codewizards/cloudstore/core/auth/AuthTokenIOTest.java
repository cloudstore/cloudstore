package co.codewizards.cloudstore.core.auth;

import static org.assertj.core.api.Assertions.*;

import java.util.Date;

import org.junit.Test;

import co.codewizards.cloudstore.core.dto.DateTime;

public class AuthTokenIOTest {
	@Test
	public void serialiseAndDeserialise() {
		AuthToken authToken = createAuthToken();

		AuthTokenIO io = new AuthTokenIO();
		byte[] authTokenData = io.serialise(authToken);
		assertThat(authTokenData).isNotNull();

		AuthToken authToken2 = io.deserialise(authTokenData);
		assertThat(authToken2).isNotNull();

		assertThat(authToken2.getExpiryDateTime()).isEqualTo(authToken.getExpiryDateTime());
		assertThat(authToken2.getPassword()).isEqualTo(authToken.getPassword());
	}

	public static AuthToken createAuthToken() {
		AuthToken authToken = new AuthToken();
		authToken.setExpiryDateTime(new DateTime(new Date()));
		authToken.setPassword("fadgfsdagasd");
		return authToken;
	}
}
