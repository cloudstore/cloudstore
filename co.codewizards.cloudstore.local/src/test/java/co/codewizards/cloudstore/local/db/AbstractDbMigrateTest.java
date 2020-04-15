package co.codewizards.cloudstore.local.db;

import org.junit.AfterClass;

import co.codewizards.cloudstore.local.AbstractTest;

public abstract class AbstractDbMigrateTest extends AbstractTest {

	@AfterClass
	public static void after_AbstractDbMigrateTest() { // make sure that it is always reset!
		disablePostgresql();
	}

}
