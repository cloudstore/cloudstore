package co.codewizards.cloudstore.ls.server.cproc;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.io.IOException;

import co.codewizards.cloudstore.core.oio.File;

public class LocalServerProcessLauncher {

	public LocalServerProcessLauncher() {
	}

	public static void main(String[] args) throws Exception {
		new LocalServerProcessLauncher().start();
	}

	public void start() throws IOException {
		String javaHome = System.getProperty("java.home");
		assertNotNull("javaHome", javaHome);

		File javaExecutableFile = createFile(javaHome, "bin", "java");

//		ProcessBuilder builder = new ProcessBuilder(
//				javaExecutableFile.getAbsolutePath(), "-jar", "org.subshare.ls.server-0.9.0-SNAPSHOT.jar");

		ProcessBuilder builder = new ProcessBuilder(
				javaExecutableFile.getAbsolutePath(),
				"-cp", "/home/mn/workspace/subshare.2/org.subshare/org.subshare.ls.server/bin",
				"org.subshare.ls.server.LocalServerMain");

		builder.redirectError(new java.io.File("/tmp/LocalServer.out"));
		builder.redirectOutput(new java.io.File("/tmp/LocalServer.out"));

		Process process = builder.start();

		try {
			Thread.sleep(60000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
