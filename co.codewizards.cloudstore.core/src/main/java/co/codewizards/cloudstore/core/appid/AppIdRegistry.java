package co.codewizards.cloudstore.core.appid;

import static co.codewizards.cloudstore.core.io.StreamUtil.castStream;
import static co.codewizards.cloudstore.core.util.AssertUtil.assertNotNull;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.util.IOUtil;

public class AppIdRegistry {

	private static final class Holder {
		public static final AppIdRegistry instance = new AppIdRegistry();
	}

	public static AppIdRegistry getInstance() {
		return Holder.instance;
	}

	private volatile AppId appId;

	protected AppIdRegistry() {
	}

	public AppId getAppIdOrFail() {
		AppId appId = this.appId;
		if (appId == null) {
			for (final AppId ai : ServiceLoader.load(AppId.class)) {
				if (appId == null || appId.getPriority() < ai.getPriority())
					appId = ai;
			}

			if (appId == null)
				throw new IllegalStateException("No AppId implementation found!");

			this.appId = appId;
		}
		return appId;
	}

	public void copyResourceResolvingAppId(final Reader reader, final Writer writer) throws IOException {
		assertNotNull(writer, "writer");
		assertNotNull(reader, "reader");

		final AppId appId = getAppIdOrFail();
		Map<String, Object> variables = new HashMap<>();
		variables.put("appId.simpleId", appId.getSimpleId());
		variables.put("appId.qualifiedId", appId.getQualifiedId());
		variables.put("appId.name", appId.getName());
		variables.put("appId.webSiteBaseUrl", appId.getWebSiteBaseUrl());
		IOUtil.replaceTemplateVariables(writer, reader, variables);
	}

	public void copyResourceResolvingAppId(final Class<?> sourceResClass, final String sourceResName, final File destinationFile) throws IOException {
		InputStream source = null;
		OutputStream destination = null;
		try{
			source = sourceResClass.getResourceAsStream(sourceResName);
			if (source == null)
				throw new FileNotFoundException("Class " + sourceResClass.getName() + " could not find resource " + sourceResName);

			if (destinationFile.exists()) {
				if (destinationFile.isFile()) {
					if (!destinationFile.canWrite())
						throw new IOException("destination file is unwriteable: " + destinationFile.getCanonicalPath());
				} else
					throw new IOException("destination is not a file: " +	destinationFile.getCanonicalPath());
			} else {
				final File parentdir = destinationFile.getAbsoluteFile().getParentFile();
				if (parentdir == null || !parentdir.exists())
					throw new IOException("destination's parent directory doesn't exist: " + destinationFile.getCanonicalPath());
				if (!parentdir.canWrite())
					throw new IOException("destination's parent directory is unwriteable: " + destinationFile.getCanonicalPath());
			}
			destination = castStream(destinationFile.createOutputStream());

			try (Reader r = new InputStreamReader(source)) {
				try (Writer w = new OutputStreamWriter(destination)) {
					copyResourceResolvingAppId(r, w);
				}
			}

		} finally {
			if (source != null)
				try { source.close(); } catch (final IOException e) { ; }

			if (destination != null)
				try { destination.close(); } catch (final IOException e) { ; }
		}
	}
}
