package co.codewizards.cloudstore.core.util;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.sql.DriverManager;
import java.sql.SQLException;

import co.codewizards.cloudstore.core.oio.File;

public class DerbyUtil {

	/**
	 * The Derby database was shut down successfully.
	 */
	private static final int DERBY_ERROR_CODE_SHUTDOWN_DATABASE_SUCCESSFULLY = 45000;
	/**
	 * The Derby database which was shut down was not running (the shut down had no effect).
	 */
	private static final int DERBY_ERROR_CODE_SHUTDOWN_DATABASE_WAS_NOT_RUNNING = 40000;

	private DerbyUtil() { }

	public static void shutdownDerbyDatabase(String connectionURL) {
		String shutdownConnectionURL = assertNotNull("connectionURL", connectionURL) + ";shutdown=true";
		try {
			DriverManager.getConnection(shutdownConnectionURL);
		} catch (SQLException e) {
			int errorCode = e.getErrorCode();
			if (DERBY_ERROR_CODE_SHUTDOWN_DATABASE_SUCCESSFULLY != errorCode &&
					DERBY_ERROR_CODE_SHUTDOWN_DATABASE_WAS_NOT_RUNNING != errorCode) {
				throw new RuntimeException(e);
			}
		}
	}

	public static void setLogFile(File file) {
		System.setProperty("derby.stream.error.file", assertNotNull("file", file).getAbsolutePath());
	}
}
