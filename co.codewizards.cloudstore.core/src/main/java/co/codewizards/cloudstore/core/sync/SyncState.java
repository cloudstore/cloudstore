package co.codewizards.cloudstore.core.sync;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.io.Serializable;
import java.net.URL;
import java.util.Date;

import co.codewizards.cloudstore.core.Severity;
import co.codewizards.cloudstore.core.dto.Error;

@SuppressWarnings("serial")
public class SyncState implements Serializable {

	private final URL url;

	private final Severity severity;

	private final String message;

	private final Error error;

	private final Date syncStarted;

	private final Date syncFinished;

	public SyncState(final URL url, final Severity severity, final String message, final Error error, final Date syncStarted, final Date syncFinished) {
		this.url = assertNotNull("url", url);
		this.severity = assertNotNull("severity", severity);
		this.message = message;
		this.error = error;
		this.syncStarted = assertNotNull("syncStarted", syncStarted);
		this.syncFinished = assertNotNull("syncFinished", syncFinished);
	}

	/**
	 * Gets the URL that was used for the sync.
	 * @return the URL that was used for the sync.
	 */
	public URL getUrl() {
		return url;
	}

	public Severity getSeverity() {
		return severity;
	}

	public String getMessage() {
		return message;
	}

	public Error getError() {
		return error;
	}

	/**
	 * Gets the timestamp when the sync referenced by this {@code SyncState} stared.
	 * @return the timestamp when the sync started. Never <code>null</code>.
	 */
	public Date getSyncStarted() {
		return syncStarted;
	}

	/**
	 * Gets the timestamp when the sync referenced by this {@code SyncState} finished.
	 * <p>
	 * Please note: This does not say anything about whether or not it completed (successfully).
	 * If there was an error, the {@link #getError() error} property is not <code>null</code>.
	 * @return the timestamp when the sync finished. Never <code>null</code>.
	 */
	public Date getSyncFinished() {
		return syncFinished;
	}
}
