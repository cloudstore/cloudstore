package co.codewizards.cloudstore.client;

import java.io.Console;
import java.io.File;
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
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import co.codewizards.cloudstore.core.config.ConfigDir;
import co.codewizards.cloudstore.core.repo.transport.RepoTransportFactoryRegistry;
import co.codewizards.cloudstore.core.util.HashUtil;
import co.codewizards.cloudstore.core.util.IOUtil;
import co.codewizards.cloudstore.rest.client.ssl.CheckServerTrustedCertificateExceptionContext;
import co.codewizards.cloudstore.rest.client.ssl.CheckServerTrustedCertificateExceptionResult;
import co.codewizards.cloudstore.rest.client.ssl.DynamicX509TrustManagerCallback;
import co.codewizards.cloudstore.rest.client.transport.RestRepoTransportFactory;

public class CloudStoreClient {

	public static final List<Class<? extends SubCommand>> subCommandClasses;
	static {
		@SuppressWarnings("unchecked")
		List<Class<? extends SubCommand>> l = Arrays.asList(
				AcceptRepoConnectionSubCommand.class,
				CancelRepoConnectionSubCommand.class,
				CreateRepoSubCommand.class,
				HelpSubCommand.class,
				RemoteSyncSubCommand.class,
				RepoInfoSubCommand.class,
				RequestRepoConnectionSubCommand.class,
				SyncSubCommand.class
				);

		subCommandClasses = Collections.unmodifiableList(l);
	};

	public static final List<SubCommand> subCommands;
	public static final Map<String, SubCommand> subCommandName2subCommand;
	static {
		try {
			ArrayList<SubCommand> l = new ArrayList<SubCommand>();
			Map<String, SubCommand> m = new HashMap<String, SubCommand>();
			for (Class<? extends SubCommand> c : subCommandClasses) {
				SubCommand subCommand = c.newInstance();
				l.add(subCommand);
				m.put(subCommand.getSubCommandName(), subCommand);
			}

			l.trimToSize();
			subCommands = Collections.unmodifiableList(l);
			subCommandName2subCommand = Collections.unmodifiableMap(m);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static final String CMD_PREFIX = "cloudstore"; // shell script (or windoof batch file)
//	static {
//		try {
//			CMD_PREFIX = "java -jar co.codewizards.cloudstore.client-" + VersionSubCommand.getVersion() + ".jar";
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		}
//	}

	static {
		RestRepoTransportFactory restRepoTransportFactory = RepoTransportFactoryRegistry.getInstance().getRepoTransportFactoryOrFail(RestRepoTransportFactory.class);
		restRepoTransportFactory.setDynamicX509TrustManagerCallbackClass(ConsoleDynamicX509TrustManagerCallback.class);
	}

	public static class ConsoleDynamicX509TrustManagerCallback implements DynamicX509TrustManagerCallback {
		@Override
		public CheckServerTrustedCertificateExceptionResult handleCheckServerTrustedCertificateException(CheckServerTrustedCertificateExceptionContext context) {
			CheckServerTrustedCertificateExceptionResult result = new CheckServerTrustedCertificateExceptionResult();
			String certificateSha1 = null;
			try {
				certificateSha1 = HashUtil.sha1ForHuman(context.getCertificateChain()[0].getEncoded());
			} catch (Exception e) {
				// we're in the console client, hence we can and should print the exception here and then exit.
				e.printStackTrace();
				System.exit(66);
			}
			while (true) {
				System.out.println("You are connecting to this server for the first time or someone is tampering with your");
				System.out.println("connection to this server!");
				System.out.println();
				System.out.println("The server presented a certificate with the following fingerprint (SHA1):");
				System.out.println();
				System.out.println("    " + certificateSha1);
				System.out.println();
				System.out.println("Please verify that this is really your server's certificate and not a man in the middle!");
				System.out.println("Your server shows its certificate's fingerprint during startup.");
				System.out.println();
				String trustedString = prompt("Do you want to register this certificate and trust this connection? (y/n) ");
				if ("y".equals(trustedString)) {
					result.setTrusted(true);
					break;
				}
				else if ("n".equals(trustedString)) {
					result.setTrusted(false);
					break;
				}
				System.err.println("Invalid input! Please enter 'y' for yes and 'n' for no!");
			}
			return result;
		}

		protected String prompt(String fmt, Object ... args) {
			Console console = System.console();
			if (console == null)
				throw new IllegalStateException("There is no system console! Cannot prompt \"" + String.format(fmt, args) + "\"!!!");

			String result = console.readLine(fmt, args);
			return result;
		}
	}

	private static final String[] stripSubCommand(String[] args)
	{
		String[] result = new String[args.length - 1];
		for (int i = 0; i < result.length; i++)
			result[i] = args[i + 1];

		return result;
	}

	/**
	 * Main method providing a command line interface (CLI) to the {@link KeyStore}.
	 *
	 * @param args the program arguments.
	 */
	public static void main(String[] args) throws Exception
	{
		initLogging();
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

					CmdLineParser parser = new CmdLineParser(subCommand);
					try {
						String[] argsWithoutSubCommand = stripSubCommand(args);
						parser.parseArgument(argsWithoutSubCommand);
						subCommand.prepare();
						subCommand.run();
						programExitStatus = 0;
					} catch (CmdLineException e) {
						// handling of wrong arguments
						programExitStatus = 2;
						displayHelp = true;
						System.err.println("Error: " + e.getMessage());
						System.err.println();
					} catch (Exception x) {
						programExitStatus = 3;
						x.printStackTrace();
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
				for (SubCommand sc : subCommands) {
					System.err.println("  " + sc.getSubCommandName());
				}
			}
			else {
				CmdLineParser parser = new CmdLineParser(subCommand);
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

		System.exit(programExitStatus);
	}

	private static void initLogging() throws IOException, JoranException {
		String logbackXmlName = "logback.client.xml";
		File logbackXmlFile = new File(ConfigDir.getInstance().getFile(), logbackXmlName);
		if (!logbackXmlFile.exists())
			IOUtil.copyResource(CloudStoreClient.class, logbackXmlName, logbackXmlFile);

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

	private static void doNothing() { }
}
