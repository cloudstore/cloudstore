package co.codewizards.cloudstore.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class VersionSubCommand extends SubCommand {

	public static String getVersion() throws IOException
	{
		Properties properties = new Properties();
		String resourceName = "/META-INF/maven/co.codewizards.cloudstore/co.codewizards.cloudstore.client/pom.properties";
		InputStream in = VersionSubCommand.class.getResourceAsStream(resourceName);
		if (in == null)
			return "UNKNOWN";

		try {
			properties.load(in);
		} catch (IOException x) {
			throw new IOException("Cannot read resource: " + resourceName, x);
		} finally {
			in.close();
		}
		String version = properties.getProperty("version");
		return version;
	}

	@Override
	public String getSubCommandName() {
		return "version";
	}

	@Override
	public String getSubCommandDescription() {
		return "Display the version of this JAR.";
	}

	@Override
	public void run() throws Exception
	{
		System.out.println(getVersion());
	}

}
