package co.codewizards.cloudstore.client;

import static co.codewizards.cloudstore.core.util.Util.*;

import co.codewizards.cloudstore.core.oio.file.File;
import java.io.IOException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import co.codewizards.cloudstore.core.config.ConfigDir;
import co.codewizards.cloudstore.core.repo.transport.RepoTransportFactoryRegistry;
import co.codewizards.cloudstore.core.updater.CloudStoreUpdaterCore;
import co.codewizards.cloudstore.core.util.DerbyUtil;
import co.codewizards.cloudstore.core.util.HashUtil;
import co.codewizards.cloudstore.core.util.IOUtil;
import co.codewizards.cloudstore.core.util.MainArgsUtil;
import co.codewizards.cloudstore.rest.client.ssl.CheckServerTrustedCertificateExceptionContext;
import co.codewizards.cloudstore.rest.client.ssl.CheckServerTrustedCertificateExceptionResult;
import co.codewizards.cloudstore.rest.client.ssl.DynamicX509TrustManagerCallback;
import co.codewizards.cloudstore.rest.client.transport.RestRepoTransportFactory;

public class CloudStoreClient {
	private static final Logger logger = LoggerFactory.getLogger(CloudStoreClient.class);

	public static final List<Class<? extends SubCommand>> subCommandClasses;
	static {
		final List<Class<? extends SubCommand>> l = Arrays.asList(
				AcceptRepoConnectionSubCommand.class,
				AfterUpdateHookSubCommand.class,
				CreateRepoSubCommand.class,
				CreateRepoAliasSubCommand.class,
				DropRepoAliasSubCommand.class,
				DropRepoConnectionSubCommand.class,
				HelpSubCommand.class,
				RepairDatabaseSubCommand.class,
				RepoInfoSubCommand.class,
				RepoListSubCommand.class,
				RequestRepoConnectionSubCommand.class,
				SyncSubCommand.class,
				VersionSubCommand.class
				);

		subCommandClasses = Collections.unmodifiableList(l);
	};

	public final List<SubCommand> subCommands;
	public final Map<String, SubCommand> subCommandName2subCommand;
	{
		try {
			final ArrayList<SubCommand> l = new ArrayList<SubCommand>();
			final Map<String, SubCommand> m = new HashMap<String, SubCommand>();
			for (final Class<? extends SubCommand> c : subCommandClasses) {
				final SubCommand subCommand = c.newInstance();
				l.add(subCommand);
				m.put(subCommand.getSubCommandName(), subCommand);
			}

			l.trimToSize();
			subCommands = Collections.unmodifiableList(l);
			subCommandName2subCommand = Collections.unmodifiableMap(m);
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static final String CMD_PREFIX = "cloudstore"; // shell script (or windoof batch file)
	private boolean throwException = true;
	/**
	 * The program arguments. Never <code>null</code>, but maybe an empty array (length 0).
	 */
	private final String[] args;

	public static class ConsoleDynamicX509TrustManagerCallback implements DynamicX509TrustManagerCallback {
		@Override
		public CheckServerTrustedCertificateExceptionResult handleCheckServerTrustedCertificateException(final CheckServerTrustedCertificateExceptionContext context) {
			final CheckServerTrustedCertificateExceptionResult result = new CheckServerTrustedCertificateExceptionResult();
			String certificateSha1 = null;
			try {
				certificateSha1 = HashUtil.sha1ForHuman(context.getCertificateChain()[0].getEncoded());
			} catch (final Exception e) {
				// we're in the console client, hence we can and should print the exception here and then exit.
				e.printStackTrace();
				System.exit(66);
			}
			System.out.println("You are connecting to this server for the first time or someone is tampering with your");
			System.out.println("connection to this server!");
			System.out.println();
			System.out.println("The server presented a certificate with the following fingerprint (SHA1):");
			System.out.println();
			System.out.println("	" + certificateSha1);
			System.out.println();
			System.out.println("Please verify that this is really your server's certificate and not a man in the middle!");
			System.out.println("Your server shows its certificate's fingerprint during startup.");
			System.out.println();
			final String trustedString = prompt(">>> Do you want to register this certificate and trust this connection? (y/n) ");
			if ("y".equals(trustedString)) {
				result.setTrusted(true);
			}
			else if ("n".equals(trustedString)) {
				result.setTrusted(false);
			}
			return result;
		}

		protected String prompt(final String question, final Object ... args) {
			final TimeoutConsoleReader consoleInput = new TimeoutConsoleReader(question, 300*1000, "n");
			String result;
			try {
				result = consoleInput.readLine();
			} catch (final InterruptedException e) {
				throw new IllegalStateException("A problem occured, while reading from console!");
			}
			return result;
		}
	}

	private static final String[] stripSubCommand(final String[] args)
	{
		final String[] result = new String[args.length - 1];
		for (int i = 0; i < result.length; i++) {
			result[i] = args[i + 1];
		}

		return result;
	}

	/**
	 * Main method providing a command line interface (CLI) to the {@link KeyStore}.
	 *
	 * @param args the program arguments.
	 */
	public static void main(final String... args) throws Exception
	{
		initLogging();
		try {
			final int programExitStatus;
			try {
				final RestRepoTransportFactory restRepoTransportFactory = RepoTransportFactoryRegistry.getInstance().getRepoTransportFactoryOrFail(RestRepoTransportFactory.class);
				restRepoTransportFactory.setDynamicX509TrustManagerCallbackClass(ConsoleDynamicX509TrustManagerCallback.class);
				programExitStatus = new CloudStoreClient(args).throwException(false).execute();
			} finally {
				// Doing it after execute(), because the system-properties are otherwise maybe not set.
				// Doing it in a finally-block, because the server might already be updated and incompatible - thus causing an error.
				// The following method catches all exceptions and logs them, hence this should not interfere with
				// the clean program completion.
				new CloudStoreUpdaterCore().createUpdaterDirIfUpdateNeeded();
			}
			System.exit(programExitStatus);
		} catch (final Throwable x) {
			logger.error(x.toString(), x);
			System.exit(999);
		}
	}

	public CloudStoreClient(final String... args) {
		this.args = args == null ? new String[0] : args;
	}

	public boolean isThrowException() {
		return throwException;
	}
	public void setThrowException(final boolean throwException) {
		this.throwException = throwException;
	}
	public CloudStoreClient throwException(final boolean throwException) {
		setThrowException(throwException);
		return this;
	}

	public int execute() throws Exception {
		logger.debug("execute: CloudStore CLI version {} is executing.", VersionSubCommand.getVersion());
		final String[] args = MainArgsUtil.extractAndApplySystemPropertiesReturnOthers(this.args);
		int programExitStatus = 1;
		boolean displayHelp = true;
		String subCommandName = null;
		SubCommand subCommand = null;

		if (args.length > 0) {
			subCommandName = args[0];

			if ("help".equals(subCommandName)) {
				if (args.length > 1) {
					subCommandName = args[1];
					subCommand = subCommandName2subCommand.get(subCommandName);
					if (subCommand == null) {
						System.err.println("Unknown sub-command: " + subCommandName);
						subCommandName = null;
					}
				}
			}
			else {
				subCommand = subCommandName2subCommand.get(subCommandName);
				if (subCommand == null) {
					System.err.println("Unknown sub-command: " + subCommandName);
					subCommandName = null;
				}
				else {
					displayHelp = false;

					final CmdLineParser parser = new CmdLineParser(subCommand);
					try {
						final String[] argsWithoutSubCommand = stripSubCommand(args);
						parser.parseArgument(argsWithoutSubCommand);
						subCommand.prepare();
						subCommand.run();
						programExitStatus = 0;
					} catch (final CmdLineException e) {
						// handling of wrong arguments
						programExitStatus = 2;
						displayHelp = true;
						System.err.println("Error: " + e.getMessage());
						System.err.println();
						if (throwException)
							throw e;
					} catch (final Exception x) {
						programExitStatus = 3;
						logger.error(x.toString(), x);
						if (throwException)
							throw x;
					}
				}
			}
		}

		if (displayHelp) {
			if (subCommand == null) {
				System.err.println("Syntax: " + CMD_PREFIX + " <sub-command> <options>");
				System.err.println();
				System.err.println("Get help for a specific sub-command: " + CMD_PREFIX + " help <sub-command>");
				System.err.println();
				System.err.println("Available sub-commands:");
				for (final SubCommand sc : subCommands) {
					if (sc.isVisibleInHelp()) {
						System.err.println("  " + sc.getSubCommandName());
					}
				}
			}
			else {
				final CmdLineParser parser = new CmdLineParser(subCommand);
				System.err.println(subCommand.getSubCommandName() + ": " + subCommand.getSubCommandDescription());
				System.err.println();
				System.err.print("Syntax: " + CMD_PREFIX + " " + subCommand.getSubCommandName());
				parser.printSingleLineUsage(System.err);
				System.err.println();
				System.err.println();
				System.err.println("Options:");
				parser.printUsage(System.err);
			}
		}

		return programExitStatus;
	}

	private static void initLogging() throws IOException, JoranException {
		final File logDir = ConfigDir.getInstance().getLogDir();
		DerbyUtil.setLogFile(newFile(logDir, "derby.log"));

		final String logbackXmlName = "logback.client.xml";
		final File logbackXmlFile = newFile(ConfigDir.getInstance().getFile(), logbackXmlName);
		if (!logbackXmlFile.exists()) {
			IOUtil.copyResource(CloudStoreClient.class, logbackXmlName, logbackXmlFile);
		}

		final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		try {
		  final JoranConfigurator configurator = new JoranConfigurator();
		  configurator.setContext(context);
		  // Call context.reset() to clear any previous configuration, e.g. default
		  // configuration. For multi-step configuration, omit calling context.reset().
		  context.reset();
		  configurator.doConfigure(logbackXmlFile);
		} catch (final JoranException je) {
			// StatusPrinter will handle this
			doNothing();
		}
		StatusPrinter.printInCaseOfErrorsOrWarnings(context);
	}
}
