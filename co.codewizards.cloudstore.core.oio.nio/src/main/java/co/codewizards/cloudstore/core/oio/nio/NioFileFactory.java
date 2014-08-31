package co.codewizards.cloudstore.core.oio.nio;

import java.io.IOException;
import java.net.URI;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.oio.FileFactory;

/**
 * @author Sebastian Schefczyk
 *
 */
public class NioFileFactory implements FileFactory {

	@Override
	public int getPriority() {
		return 10;
	}

	@Override
	public File createFile(final String pathname) {
		return new NioFile(pathname);
	}

	@Override
	public File createFile(final String parent, final String child) {
		return new NioFile(parent, child);
	}

	@Override
	public File createFile(final File parent, final String child) {
		return new NioFile(parent, child);
	}

	@Override
	public File createFile(final java.io.File file) {
		return new NioFile(file);
	}

	@Override
	public File createFile(final URI uri) {
		return new NioFile(uri);
	}


	@Override
	public File createTempDirectory(final String prefix) throws IOException {
		return NioFileUtil.createTempDirectory(prefix);
	}

	@Override
	public File createTempFile(final String prefix, final String suffix) throws IOException {
		return NioFileUtil.createTempFile(prefix, suffix);
	}

	@Override
	public File createTempFile(final String prefix, final String suffix, final File parentDir) throws IOException {
		return NioFileUtil.createTempFile(prefix, suffix, parentDir);
	}

}
