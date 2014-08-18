package co.codewizards.cloudstore.oio.nio;

import java.io.IOException;
import java.net.URI;

import co.codewizards.cloudstore.core.oio.file.File;
import co.codewizards.cloudstore.core.oio.file.FileFactoryService;

/**
 * @author Sebastian Schefczyk
 *
 */
public class NioFileFactoryService implements FileFactoryService {

	@Override
	public int getPriority() {
		return 10;
	}


	@Override
	public File createNewFile(final String pathname) {
		return new NioFile(pathname);
	}

	@Override
	public File createNewFile(final String parent, final String child) {
		return new NioFile(parent, child);
	}

	@Override
	public File createNewFile(final File parent, final String child) {
		return new NioFile(parent, child);
	}

	@Override
	public File createNewFile(final java.io.File file) {
		return new NioFile(file);
	}

	@Override
	public File createNewFile(final URI uri) {
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
	public File createTempFile(final String prefix, final String suffix, final File dir) throws IOException {
		return NioFileUtil.createTempFile(prefix, suffix, dir);
	}

}
