package co.codewizards.cloudstore.core.sync;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.io.Serializable;
import java.net.URL;

import co.codewizards.cloudstore.core.Severity;
import co.codewizards.cloudstore.core.dto.Error;

public class SyncState implements Serializable {
	private static final long serialVersionUID = 1L;

	private final URL url;

	private final Severity severity;

	private final String message;

	private final Error error;

	public SyncState(final URL url, final Severity severity, final String message, final Error error) {
		this.url = assertNotNull("url", url);
		this.severity = assertNotNull("severity", severity);
		this.message = message;
		this.error = error;
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
}
