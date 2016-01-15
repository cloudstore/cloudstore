package co.codewizards.cloudstore.ls.server.cproc;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class LocalServerMain {
	protected LocalServerMain() {
	}

	public static void main(String[] args) throws Exception {
		File simpleLogFile = new File("/tmp/localServer.log");
		for (int i = 0; i < 600; ++i) {
			try (OutputStream out = new FileOutputStream(simpleLogFile, true);
					OutputStreamWriter w = new OutputStreamWriter(out, StandardCharsets.UTF_8);) {
				w.write(new Date().toString());
				w.write('\n');
			}

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				doNothing();
			}
		}
	}
}
