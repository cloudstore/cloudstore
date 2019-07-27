package co.codewizards.cloudstore.ls.server.cproc;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.Util.*;
import static java.util.Objects.*;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import co.codewizards.cloudstore.core.appid.AppIdRegistry;
import co.codewizards.cloudstore.core.config.ConfigDir;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.util.DerbyUtil;
import co.codewizards.cloudstore.core.util.MainArgsUtil;
import co.codewizards.cloudstore.ls.server.LocalServer;

public class LocalServerMain {
	private static Class<? extends LocalServer> localServerClass = LocalServer.class;

	private static final Logger logger = LoggerFactory.getLogger(LocalServerMain.class);

	protected LocalServerMain() {
	}

	public static void main(String[] args) throws Exception {
		initLogging();

		try {
			args = MainArgsUtil.extractAndApplySystemPropertiesReturnOthers(args);
			final LocalServer localServer = createLocalServer();
			localServer.setLocalServerStopFileEnabled(true);
			localServer.start();
		} catch (final Throwable x) {
			logger.error(x.toString(), x);
			System.exit(999);
		}
	}

	public static Class<? extends LocalServer> getLocalServerClass() {
		return localServerClass;
	}
	public static void setLocalServerClass(final Class<? extends LocalServer> localServerClass) {
		LocalServerMain.localServerClass = requireNonNull(localServerClass, "localServerClass");
	}

	protected static Constructor<? extends LocalServer> getLocalServerConstructor() throws NoSuchMethodException, SecurityException {
		final Class<? extends LocalServer> clazz = getLocalServerClass();
		final Constructor<? extends LocalServer> constructor = clazz.getConstructor();
		return constructor;
	}

	protected static LocalServer createLocalServer() throws NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		final Constructor<? extends LocalServer> constructor = getLocalServerConstructor();
		final LocalServer cloudStoreServer = constructor.newInstance();
		return cloudStoreServer;
	}

	private static void initLogging() throws IOException, JoranException {
		final File logDir = ConfigDir.getInstance().getLogDir();
		DerbyUtil.setLogFile(createFile(logDir, "derby.log"));

		final String logbackXmlName = "logback.localserver.xml";
		final File logbackXmlFile = createFile(ConfigDir.getInstance().getFile(), logbackXmlName);
		if (!logbackXmlFile.exists()) {
			AppIdRegistry.getInstance().copyResourceResolvingAppId(
					LocalServerMain.class, logbackXmlName, logbackXmlFile);
		}

		final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
	    try {
	      final JoranConfigurator configurator = new JoranConfigurator();
	      configurator.setContext(context);
	      // Call context.reset() to clear any previous configuration, e.g. default
	      // configuration. For multi-step configuration, omit calling context.reset().
	      context.reset();
	      configurator.doConfigure(logbackXmlFile.getIoFile());
	    } catch (final JoranException je) {
	    	// StatusPrinter will handle this
	    	doNothing();
	    }
	    StatusPrinter.printInCaseOfErrorsOrWarnings(context);
	}
}
