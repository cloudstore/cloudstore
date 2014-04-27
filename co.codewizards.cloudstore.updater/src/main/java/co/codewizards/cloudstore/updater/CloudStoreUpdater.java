package co.codewizards.cloudstore.updater;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import co.codewizards.cloudstore.core.config.ConfigDir;
import co.codewizards.cloudstore.core.util.IOUtil;
import co.codewizards.cloudstore.core.util.PropertiesUtil;

public class CloudStoreUpdater {
	private static final Logger logger = LoggerFactory.getLogger(CloudStoreUpdater.class);

	private static final String INSTALLATION_PROPERTIES_FILE_NAME = "installation.properties";
	private static final String INSTALLATION_PROPERTIES_ARTIFACT_ID = "artifactId";
	private static final String remoteVersionURL = "http://cloudstore.codewizards.co/update/${artifactId}/version";
	private static final String remoteUpdatePropertiesURL = "http://cloudstore.codewizards.co/update/${artifactId}/update.properties";

	private final String[] args;
	private boolean throwException = true;
	private Properties installationProperties;

	@Option(name="-installationDir", required=true, usage="Base-directory of the installation containing the 'bin' directory as well as the 'installation.properties' file - e.g. '/opt/cloudstore'. The installation in this directory will be updated.")
	private String installationDir;
	private File installationDirFile;

	private String remoteVersion;
	private Properties remoteUpdateProperties;
	private File tempDownloadDir;

	public static void main(String[] args) throws Exception {
		initLogging();
		try {
			int programExitStatus = new CloudStoreUpdater(args).throwException(false).execute();
			System.exit(programExitStatus);
		} catch (Throwable x) {
			logger.error(x.toString(), x);
			System.exit(999);
		}
	}

	public CloudStoreUpdater(String[] args) {
		this.args = args;
	}

	public boolean isThrowException() {
		return throwException;
	}
	public void setThrowException(boolean throwException) {
		this.throwException = throwException;
	}
	public CloudStoreUpdater throwException(boolean throwException) {
		setThrowException(throwException);
		return this;
	}

	public int execute() throws Exception {
		int programExitStatus = 1;
		CmdLineParser parser = new CmdLineParser(this);
		try {
			parser.parseArgument(args);
			this.run();
			programExitStatus = 0;
		} catch (CmdLineException e) {
			// handling of wrong arguments
			programExitStatus = 2;
			System.err.println("Error: " + e.getMessage());
			System.err.println();
			if (throwException)
				throw e;
		} catch (Exception x) {
			programExitStatus = 3;
			logger.error(x.toString(), x);
			if (throwException)
				throw x;
		}
		return programExitStatus;
	}

	private static void initLogging() throws IOException, JoranException {
		ConfigDir.getInstance().getLogDir();

		final String logbackXmlName = "logback.updater.xml";
		File logbackXmlFile = new File(ConfigDir.getInstance().getFile(), logbackXmlName);
		if (!logbackXmlFile.exists())
			IOUtil.copyResource(CloudStoreUpdater.class, logbackXmlName, logbackXmlFile);

		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
	    try {
	      JoranConfigurator configurator = new JoranConfigurator();
	      configurator.setContext(context);
	      // Call context.reset() to clear any previous configuration, e.g. default
	      // configuration. For multi-step configuration, omit calling context.reset().
	      context.reset();
	      configurator.doConfigure(logbackXmlFile);
	    } catch (JoranException je) {
	    	// StatusPrinter will handle this
	    	doNothing();
	    }
	    StatusPrinter.printInCaseOfErrorsOrWarnings(context);
	}

	private void run() throws Exception {
		try {
			final File downloadFile = downloadURLViaRemoteUpdateProperties("artifact[${artifactId}].downloadURL");
			final File signatureFile = downloadURLViaRemoteUpdateProperties("artifact[${artifactId}].signatureURL");

			new PGPVerifier().verify(downloadFile, signatureFile);

			// TODO backup the installation directory.

			// TODO delete the entire old installation (or alternatively delete every file not being overwritten by the next step).

			// TODO overwrite the installation directory instead of this test dir "extract".
			final File extractRootDir = new File(getTempDownloadDir(), "extract");
			extractTarGz(downloadFile, extractRootDir);
		} finally {
			if (tempDownloadDir != null)
				IOUtil.deleteDirectoryRecursively(tempDownloadDir);
		}
	}

	private void extractTarGz(final File tarGzFile, final File extractRootDir) throws IOException {
		extractRootDir.mkdir();

		final FileInputStream fin = new FileInputStream(tarGzFile);
		try {
			final TarArchiveInputStream in = new TarArchiveInputStream(new GzipCompressorInputStream(new BufferedInputStream(fin)));
			try {
				TarArchiveEntry entry;
				while (null != (entry = in.getNextTarEntry())) {
					if(entry.isDirectory()) {
						// create the directory
						final File dir = new File(extractRootDir, entry.getName());
						if (!dir.exists() && !dir.mkdirs())
							throw new IllegalStateException("Could not create directory entry, possibly permission issues: " + dir.getAbsolutePath());
					}
					else {
						final File file = new File(extractRootDir, entry.getName());

						final File dir = file.getParentFile();
						if (!dir.isDirectory())
							dir.mkdirs( );

						BufferedOutputStream out = new BufferedOutputStream( new FileOutputStream(file) );
						try {
							int len;
							byte[] buf = new byte[1024 * 16];
							while( (len = in.read(buf)) > 0 ) {
								if (len > 0)
									out.write(buf, 0, len);
							}
						} finally {
							out.close();
						}

						if ((entry.getMode() & 1) != 0)
							file.setExecutable(true);
					}
				}
			} finally {
				in.close();
			}
		} finally {
			fin.close();
		}
	}

	private File downloadURLViaRemoteUpdateProperties(final String remoteUpdatePropertiesKey) {
		final String resolvedKey = resolve(remoteUpdatePropertiesKey);
		final String urlStr = getRemoteUpdateProperties().getProperty(resolvedKey);
		if (urlStr == null || urlStr.trim().isEmpty())
			throw new IllegalStateException("No value for key in remoteUpdateProperties: " + resolvedKey);

		final String resolvedURLStr = resolve(urlStr);

		final File tempDownloadDir = getTempDownloadDir();

		try {
			final URL url = new URL(resolvedURLStr);
			final String path = url.getPath();
			final int lastSlashIndex = path.lastIndexOf('/');
			if (lastSlashIndex < 0)
				throw new IllegalStateException("No '/' found in URL?!");

			final String fileName = path.substring(lastSlashIndex + 1);
			final File downloadFile = new File(tempDownloadDir, fileName);

			boolean successful = false;
			final InputStream in = url.openStream();
			try {
				final FileOutputStream out = new FileOutputStream(downloadFile);
				try {
					IOUtil.transferStreamData(in, out);
				} finally {
					out.close();
				}
				successful = true;
			} finally {
				in.close();

				if (!successful)
					downloadFile.delete();
			}

			return downloadFile;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private File getTempDownloadDir() {
		if (tempDownloadDir == null) {
			try {
				tempDownloadDir = IOUtil.createUniqueRandomFolder(IOUtil.getTempDir(), "cloudstore-update-");
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return tempDownloadDir;
	}

	private File getInstallationDir() {
		if (installationDirFile == null) {
			final File f = new File(assertNotNull("installationDir", installationDir));
			if (!f.exists())
				throw new IllegalArgumentException(String.format("installationDir '%s' does not exist!", installationDir));

			if (!f.isDirectory())
				throw new IllegalArgumentException(String.format("installationDir '%s' is not a directory!", installationDir));

			installationDirFile = f;
		}
		return installationDirFile;
	}

	private Properties getInstallationProperties() {
		if (installationProperties == null) {
			final File installationPropertiesFile = new File(getInstallationDir(), INSTALLATION_PROPERTIES_FILE_NAME);
			if (!installationPropertiesFile.exists())
				throw new IllegalArgumentException(String.format("installationPropertiesFile '%s' does not exist!", installationPropertiesFile.getAbsolutePath()));

			if (!installationPropertiesFile.isFile())
				throw new IllegalArgumentException(String.format("installationPropertiesFile '%s' is not a file!", installationPropertiesFile.getAbsolutePath()));

			try {
				final Properties properties = PropertiesUtil.load(installationPropertiesFile);
				installationProperties = properties;
			} catch (IOException x) {
				throw new RuntimeException(x);
			}
		}
		return installationProperties;
	}

	private String resolve(String template) {
		final String artifactId = getInstallationProperties().getProperty(INSTALLATION_PROPERTIES_ARTIFACT_ID);
		assertNotNull("artifactId", artifactId);

		final String version = getRemoteVersion();

		final Map<String, Object> variables = new HashMap<>(2);
		variables.put("artifactId", artifactId);
		variables.put("version", version);
		return IOUtil.replaceTemplateVariables(template, variables);
	}

	private Properties getRemoteUpdateProperties() {
		if (remoteUpdateProperties == null) {
			final String resolvedRemoteUpdatePropertiesURL = resolve(remoteUpdatePropertiesURL);
			final Properties properties = new Properties();
			try {
				final URL url = new URL(resolvedRemoteUpdatePropertiesURL);
				final InputStream in = url.openStream();
				try {
					properties.load(in);
				} finally {
					in.close();
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			remoteUpdateProperties = properties;
		}
		return remoteUpdateProperties;
	}

	private String getRemoteVersion() {
		if (remoteVersion == null) {
			final String artifactId = getInstallationProperties().getProperty(INSTALLATION_PROPERTIES_ARTIFACT_ID);
			// cannt use resolve(...), because it invokes this method ;-)
			assertNotNull("artifactId", artifactId);
			final Map<String, Object> variables = new HashMap<>(1);
			variables.put("artifactId", artifactId);
			final String resolvedRemoteVersionURL = IOUtil.replaceTemplateVariables(remoteVersionURL, variables);
			try {
				final URL url = new URL(resolvedRemoteVersionURL);
				final InputStream in = url.openStream();
				try {
					BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"));
					final String line = r.readLine();
					if (line == null || line.isEmpty())
						throw new IllegalStateException("Failed to read version from: " + resolvedRemoteVersionURL);

					remoteVersion = line;
					r.close();
				} finally {
					in.close();
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return remoteVersion;
	}

	private static final void doNothing() { }
}
