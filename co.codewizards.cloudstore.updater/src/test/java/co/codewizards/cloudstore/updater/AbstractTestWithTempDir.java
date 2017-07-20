package co.codewizards.cloudstore.updater;

import static co.codewizards.cloudstore.core.io.StreamUtil.castStream;
import static co.codewizards.cloudstore.core.oio.OioFileFactory.createTempDirectory;
import static co.codewizards.cloudstore.core.util.IOUtil.transferStreamData;
import static co.codewizards.cloudstore.core.util.StringUtil.isEmpty;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.oio.File;

public abstract class AbstractTestWithTempDir {
	
	private static final Logger logger = LoggerFactory.getLogger(AbstractTestWithTempDir.class);

	protected File tempDir;

	@Before
	public void before() throws Exception {
		tempDir = createTempDirectory("cloudstore-test-");
	}

	@After
	public void after() throws Exception {
		File td = tempDir;
		tempDir = null;

		if (td != null)
			td.deleteRecursively();
	}

	protected File downloadFileToTempDir(String urlStr) throws IOException {
		logger.info("downloadFileToTempDir: {}", urlStr);
		long startTimestamp = System.currentTimeMillis();
		URL url = new URL(urlStr);

		String fileName = url.getPath();
		int lastSlash = fileName.lastIndexOf('/');
		if (lastSlash < 0)
			throw new IllegalArgumentException("urlStr's path does not contain a '/': " + urlStr);

		fileName = fileName.substring(lastSlash + 1);
		if (isEmpty(fileName))
			throw new IllegalArgumentException("urlStr's path ends on '/': " + urlStr);

		File file = tempDir.createFile(fileName);
		try (InputStream in = url.openStream();) {
			try (OutputStream out = castStream(file.createOutputStream())) {
				transferStreamData(in, out);
			}
		}
		logger.info("downloadFileToTempDir: Download took {} ms: {}", System.currentTimeMillis() - startTimestamp, urlStr);
		return file;
	}

}
