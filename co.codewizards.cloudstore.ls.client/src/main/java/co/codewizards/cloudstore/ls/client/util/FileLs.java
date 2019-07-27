package co.codewizards.cloudstore.ls.client.util;

import static java.util.Objects.*;

import co.codewizards.cloudstore.core.io.IInputStream;
import co.codewizards.cloudstore.core.io.IOutputStream;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.ls.client.LocalServerClient;

/**
 * Utility class for operating on a {@link File} instance inside the LocalServer's VM.
 * <p>
 * Most importantly, this is used to create instances of {@link IInputStream} or
 * {@link IOutputStream} inside the LocalServer's VM.
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public class FileLs {

	private FileLs() {
	}

	public static IInputStream createInputStream(final File file) {
		requireNonNull(file, "file");
		IInputStream in = LocalServerClient.getInstance().invoke(file, "createInputStream");
		return in;
	}

	public static IOutputStream createOutputStream(final File file) {
		requireNonNull(file, "file");
		IOutputStream out = LocalServerClient.getInstance().invoke(file, "createOutputStream");
		return out;
	}

	public static IOutputStream createOutputStream(final File file, boolean append) {
		requireNonNull(file, "file");
		IOutputStream out = LocalServerClient.getInstance().invoke(file, "createOutputStream", append);
		return out;
	}
}
