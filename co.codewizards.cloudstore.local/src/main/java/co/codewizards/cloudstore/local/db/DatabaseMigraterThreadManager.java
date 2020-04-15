package co.codewizards.cloudstore.local.db;

import static java.util.Objects.*;

import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseMigraterThreadManager {
	private static final Logger logger = LoggerFactory.getLogger(DatabaseMigraterThreadManager.class);

	private static final class Holder {
		protected static DatabaseMigraterThreadManager instance = new DatabaseMigraterThreadManager();
	}

	private final AtomicLong nextId = new AtomicLong();

	protected DatabaseMigraterThreadManager() {
	}

	public static DatabaseMigraterThreadManager getInstance() {
		return Holder.instance;
	}

	public void launch(final DatabaseMigrater databaseMigrater) {
		requireNonNull(databaseMigrater, "databaseMigrater");

		new Thread() {
			{
				setName("DatabaseMigraterThread-" + nextId.getAndIncrement());
				setDaemon(false);
			}

			@Override
			public void run() {
				try {
						databaseMigrater.migrateIfNeeded();
				} catch (Throwable error) {
					logger.error("run: " + error, error);
				}
			}

		}.start();
	}

}
