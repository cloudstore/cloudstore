package co.codewizards.cloudstore.core.util;

import static org.junit.Assert.*;

import java.io.File;
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
		URL input = new URL("http", "codewizwards.co", 123, "/asdf/qwer/#Yxcv");
		assertEquals(input.toExternalForm(), "http://codewizwards.co:123/asdf/qwer/#Yxcv");
		//anchors/fragments will be removed!
		URL result = UrlUtil.canonicalizeURL(input);
		String expectedResult = "http://codewizwards.co:123/asdf/qwer";
		assertEquals(expectedResult, result.toExternalForm());
	}

	@Test
	public void canonicalizeURL_http_escaped() throws MalformedURLException {
		URL input = new URL("http", "codewizwards.co", 123, "/asdf/qwer/%23Yxcv");
		assertEquals(input.toExternalForm(), "http://codewizwards.co:123/asdf/qwer/%23Yxcv");
		URL result = UrlUtil.canonicalizeURL(input);
		assertEquals(input, result);
	}

	@Test
	public void canonicalizeURL_file_escaped() throws MalformedURLException {
		String fileString = "file:/tmp/asdf/qwer/\\#Yxcv";
		URL input = new URL(fileString);
		assertEquals(input.toExternalForm(), fileString);
		URL result = UrlUtil.canonicalizeURL(input);
		assertEquals(input, result);
	}

	@Test
	public void canonicalizeURL_file_encoded() throws MalformedURLException {
		String fileString = "file:/tmp/asdf/qwer/%23Yxcv";
		URL input = new URL(fileString);
		assertEquals(input.toExternalForm(), fileString);
		URL result = UrlUtil.canonicalizeURL(input);
		assertEquals(input, result);
	}

	@Test
	public void canonicalizeURL_file_unescaped() throws MalformedURLException {
		String fileString = "file:/tmp/asdf/qwer/#Yxcv";
		URL input = new URL(fileString);
		assertEquals(input.toExternalForm(), fileString);
		URL result = UrlUtil.canonicalizeURL(input);
		String expectedResult = "file:/tmp/asdf/qwer";
		assertEquals(expectedResult, result.toExternalForm());
	}


	@Test
	public void appendPath_encoded() throws Exception {
		String encodedPath = "/%23Tag"; //%23 is the '#' character
		URI uri = new URI("file", "/tmp/UrlUtilTest/parentFolder", null);
		String expected = "file:/tmp/UrlUtilTest/parentFolder/%23Tag";
		URI actual = UrlUtil.appendPath(uri, encodedPath, true);
		assertEquals(expected, actual.toString());
	}

	@Test
	public void appendPath_decoded() throws Exception {
		String unencodedPath = "/#Tag"; //%23 is the '#' character
		URI uri = new URI("file", "/tmp/UrlUtilTest/parentFolder", null);
		String expected = "file:/tmp/UrlUtilTest/parentFolder/%23Tag";
		URI actual = UrlUtil.appendPath(uri, unencodedPath, false);
		assertEquals(expected, actual.toString());
	}

	@Test
	public void getFile_url() throws Exception {
		File tmpDir = Files.createTempDirectory(this.getClass().getSimpleName()).toFile();

		File fileFromString = new File(tmpDir, "#Cumbia");
		if (!fileFromString.exists() && fileFromString.createNewFile())
			System.out.println("File created: " + fileFromString.getCanonicalPath());
		else
			System.out.println("File already existed: " + fileFromString.getCanonicalPath());

		URL url = new URL("file:" + tmpDir.getCanonicalPath() + "/" + "%23Cumbia");
		File file = UrlUtil.getFile(url);
		assertTrue("File does not exist: " + file.getCanonicalPath(), file.exists());
		assertEquals(fileFromString, file);
	}

	@Test
	public void getFile_fileString() throws Exception {
		File tmpDir = Files.createTempDirectory(this.getClass().getSimpleName()).toFile();

		File fileFromString = new File(tmpDir, "#Cumbia");
		if (!fileFromString.exists() && fileFromString.createNewFile())
			System.out.println("File created: " + fileFromString.getCanonicalPath());
		else
			System.out.println("File already existed: " + fileFromString.getCanonicalPath());

		File file = UrlUtil.getFile(tmpDir, "/%23Cumbia");
		assertTrue(file.exists());
		assertEquals(fileFromString, file);
	}

}
