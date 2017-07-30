package co.codewizards.cloudstore.core.dto;

import static org.assertj.core.api.Assertions.*;

import java.sql.SQLException;

import org.junit.Test;

public class RemoteExceptionUtilTest {

	@Test
	public void simpleIllegalArgumentException() {
		IllegalArgumentException origEx = new IllegalArgumentException("bla > 0");
		Error error = new Error(origEx);
		try {
			RemoteExceptionUtil.throwOriginalExceptionIfPossible(error);
		} catch (Exception newEx) {
			newEx.printStackTrace();
			assertThat(newEx).isInstanceOf(origEx.getClass());
			assertThat(newEx.getMessage()).isEqualTo("bla > 0");
		}
	}

	@Test
	public void nestedSQLException() {
		SQLException origNestedEx = createSQLException("blablubbxxx");
		RuntimeException origEx = createRuntimeException(origNestedEx);
		assertThat(origEx.getMessage()).isEqualTo("java.sql.SQLException: blablubbxxx");

		Error error = new Error(origEx);
		try {
			RemoteExceptionUtil.throwOriginalExceptionIfPossible(error);
		} catch (Exception newEx) {
			newEx.printStackTrace();
			assertThat(newEx).isInstanceOf(RuntimeException.class);
			assertThat(newEx.getMessage()).isEqualTo("java.sql.SQLException: blablubbxxx");
			assertThat(newEx.getCause()).isNotNull();
			assertThat(newEx.getCause()).isInstanceOf(SQLException.class);
			assertThat(newEx.getCause().getMessage()).isEqualTo("blablubbxxx");
		}
	}

	private RuntimeException createRuntimeException(Throwable cause) {
		return new RuntimeException(cause);
	}

	private SQLException createSQLException(String message) {
		return new SQLException(message);
	}

}
