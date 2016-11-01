package co.codewizards.cloudstore.core.version;

import static co.codewizards.cloudstore.core.io.StreamUtil.*;
import static co.codewizards.cloudstore.core.objectfactory.ObjectFactoryUtil.*;
import static co.codewizards.cloudstore.core.util.UrlUtil.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import co.codewizards.cloudstore.core.oio.File;

public class LocalVersionInIdeHelper {

	protected URL resource;

	protected LocalVersionInIdeHelper() {
	}

	public static LocalVersionInIdeHelper getInstance() {
		return createObject(LocalVersionInIdeHelper.class);
	}

	public Version getLocalVersionInIde() {
		resource = this.getClass().getResource("");

		if (resource.getProtocol().equalsIgnoreCase(PROTOCOL_JAR)) {
			return getLocalVersionInIde_jar();
		} else if (resource.getProtocol().equalsIgnoreCase(PROTOCOL_FILE)) {
			return getLocalVersionInIde_file();
		} else
			throw new IllegalStateException("LocalVersionInIdeHelper was not loaded from a local JAR or class file!");
	}

	protected Version getLocalVersionInIde_jar() {
		final String pomXmlResourceName = "META-INF/maven/co.codewizards.cloudstore/co.codewizards.cloudstore.core/pom.xml";
		resource = this.getClass().getClassLoader().getResource(pomXmlResourceName);
		if (resource == null)
			throw new IllegalStateException("Resource not found in JAR: " + pomXmlResourceName);

		try {
			try (InputStream pomXmlIn = resource.openStream()) {
				return readVersionFromPomXml(pomXmlIn);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	protected Version getLocalVersionInIde_file() {
		try {
			File dir = getFile(resource).getCanonicalFile();
			File pomXmlFile;
			do {
				pomXmlFile = null;
				dir = dir.getParentFile();
				if (dir == null)
					break;

				pomXmlFile = dir.createFile("pom.xml");
			} while (! pomXmlFile.exists());

			if (pomXmlFile != null) {
				try (InputStream pomXmlIn = castStream(pomXmlFile.createInputStream())) {
					return readVersionFromPomXml(pomXmlIn);
				}
			}
			throw new IllegalStateException("Could not determine local version!");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	protected Version readVersionFromPomXml(InputStream pomXmlIn) throws IOException {
		// quick'n'dirty implementation
		final Pattern pattern = Pattern.compile("<version>([^<]*)</version>");
		BufferedReader reader = new BufferedReader(new InputStreamReader(pomXmlIn, StandardCharsets.UTF_8));
		Matcher matcher = null;
		String line;
		while (null != (line = reader.readLine())) {
			if (matcher == null)
				matcher = pattern.matcher(line);
			else
				matcher.reset(line);

			if (matcher.find()) {
				String versionString = matcher.group(1);
				return new Version(versionString);
			}
		}
		throw new IllegalStateException("Could not find version inside pom.xml!");
	}
}
