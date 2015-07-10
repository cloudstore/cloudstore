package co.codewizards.cloudstore.core.oio;

import java.io.IOException;
import java.net.URI;


/**
 * @author Sebastian Schefczyk
 */
public class IoFileFactory implements FileFactory {

	/** NEVER USE DIRECTLY!!!
	 * <p/>
	 * Use OioRegistry.getInstance().getFileFactory()
	 * <p/>
	 * Must be visible for the ServiceLoader. */
	@Deprecated
	public IoFileFactory() {}

	@Override
	public int getPriority() {
		return 0;
	}

	@Override
	public File createFile(final String pathname) {
		return new IoFile(pathname);
	}

	@Override
	public File createFile(final String parent, final String child) {
		return new IoFile(parent, child);
	}

	@Override
	public File createFile(final File parent, final String child) {
		return new IoFile(parent, child);
	}

	@Override
	public File createFile(final java.io.File file) {
		return new IoFile(file);
	}

	@Override
	public File createFile(final URI uri) {
		return new IoFile(uri);
	}


	@Override
	public File createTempDirectory(final String prefix) throws IOException {
		return IoFileUtil.createTempDirectory(prefix);
	}

	@Override
	public File createTempFile(final String prefix, final String suffix) throws IOException {
		return IoFileUtil.createTempFile(prefix, suffix);
	}

	@Override
	public File createTempFile(final String prefix, final String suffix, final File parentDir) throws IOException {
		return IoFileUtil.createTempFile(prefix, suffix, parentDir);
	}

	@Override
	public File[] listRoots() {
		return IoFileUtil.listRoots();
	}
}
