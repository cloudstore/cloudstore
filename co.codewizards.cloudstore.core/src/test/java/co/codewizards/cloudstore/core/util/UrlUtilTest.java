package co.codewizards.cloudstore.core.util;

import static org.junit.Assert.*;

import co.codewizards.cloudstore.core.oio.file.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;

import org.junit.Test;

/**
 * @author Sebastian Schefczyk
 */
public class UrlUtilTest {

	@Test
	public void canonicalizeURL_http_removeFragments() throws MalformedURLException {
		final URL input = new URL("http", "codewizwards.co", 123, "/asdf/qwer/#Yxcv");
		assertEquals(input.toExternalForm(), "http://codewizwards.co:123/asdf/qwer/#Yxcv");
		//anchors/fragments will be removed!
		final URL result = UrlUtil.canonicalizeURL(input);
		final String expectedResult = "http://codewizwards.co:123/asdf/qwer";
		assertEquals(expectedResult, result.toExternalForm());
	}

	@Test
	public void canonicalizeURL_http_escaped() throws MalformedURLException {
		final URL input = new URL("http", "codewizwards.co", 123, "/asdf/qwer/%23Yxcv");
		assertEquals(input.toExternalForm(), "http://codewizwards.co:123/asdf/qwer/%23Yxcv");
		final URL result = UrlUtil.canonicalizeURL(input);
		assertEquals(input, result);
	}

	@Test
	public void canonicalizeURL_file_escaped() throws MalformedURLException {
		final String fileString = "file:/tmp/asdf/qwer/\\#Yxcv";
		final URL input = new URL(fileString);
		assertEquals(input.toExternalForm(), fileString);
		final URL result = UrlUtil.canonicalizeURL(input);
		assertEquals(input, result);
	}

	@Test
	public void canonicalizeURL_file_encoded() throws MalformedURLException {
		final String fileString = "file:/tmp/asdf/qwer/%23Yxcv";
		final URL input = new URL(fileString);
		assertEquals(input.toExternalForm(), fileString);
		final URL result = UrlUtil.canonicalizeURL(input);
		assertEquals(input, result);
	}

	@Test
	public void canonicalizeURL_file_unescaped() throws MalformedURLException {
		final String fileString = "file:/tmp/asdf/qwer/#Yxcv";
		final URL input = new URL(fileString);
		assertEquals(input.toExternalForm(), fileString);
		final URL result = UrlUtil.canonicalizeURL(input);
		final String expectedResult = "file:/tmp/asdf/qwer";
		assertEquals(expectedResult, result.toExternalForm());
	}

	@Test
	public void appendPath_encoded() throws Exception {
		final String encodedPath = "/%23Tag"; //%23 is the '#' character
		final URL url = new URI("file", "/tmp/UrlUtilTest/parentFolder", null).toURL();
		final String expected = "file:/tmp/UrlUtilTest/parentFolder/%23Tag";
		final URL actual = UrlUtil.appendEncodedPath(url, encodedPath);
		assertEquals(expected, actual.toString());
	}

	@Test
	public void appendPath_decoded() throws Exception {
		final String unencodedPath = "/#Tag"; //%23 is the '#' character
		final URL url = new URI("file", "/tmp/UrlUtilTest/parentFolder", null).toURL();
		final String expected = "file:/tmp/UrlUtilTest/parentFolder/%23Tag";
		final URL actual = UrlUtil.appendNonEncodedPath(url, unencodedPath);
		assertEquals(expected, actual.toString());
	}

	@Test
	public void getFile_url() throws Exception {
		final File tmpDir = Files.createTempDirectory(this.getClass().getSimpleName()).toFile();

		final File fileFromString = newFile(tmpDir, "#Cumbia");
		if (!fileFromString.exists() && fileFromString.createNewFile())
			System.out.println("File created: " + fileFromString.getCanonicalPath());
		else
			System.out.println("File already existed: " + fileFromString.getCanonicalPath());

		final URL url = new URL("file:" + tmpDir.getCanonicalPath() + "/" + "%23Cumbia");
		final File file = UrlUtil.getFile(url);
		assertTrue("File does not exist: " + file.getCanonicalPath(), file.exists());
		assertEquals(fileFromString, file);
	}

	@Test
	public void appendEncodedPath_getFile() throws Exception {
		final File tmpDir = Files.createTempDirectory(this.getClass().getSimpleName()).toFile();

		final File fileFromString = newFile(tmpDir, "#Cumbia");
		if (!fileFromString.exists() && fileFromString.createNewFile())
			System.out.println("File created: " + fileFromString.getCanonicalPath());
		else
			System.out.println("File already existed: " + fileFromString.getCanonicalPath());

		final URL url = UrlUtil.appendEncodedPath(tmpDir.toURI().toURL(), "%23Cumbia");

		final File file = UrlUtil.getFile(url);
		assertTrue(file.exists());
		assertEquals(fileFromString, file);
	}

	@Test
	public void appendNonEncodedPath_getFile() throws Exception {
		final File tmpDir = Files.createTempDirectory(this.getClass().getSimpleName()).toFile();

		final File fileFromString = newFile(tmpDir, "#Cumbia");
		if (!fileFromString.exists() && fileFromString.createNewFile())
			System.out.println("File created: " + fileFromString.getCanonicalPath());
		else
			System.out.println("File already existed: " + fileFromString.getCanonicalPath());

		final URL url = UrlUtil.appendNonEncodedPath(tmpDir.toURI().toURL(), "#Cumbia");

		final File file = UrlUtil.getFile(url);
		assertTrue(file.exists());
		assertEquals(fileFromString, file);
	}
}
