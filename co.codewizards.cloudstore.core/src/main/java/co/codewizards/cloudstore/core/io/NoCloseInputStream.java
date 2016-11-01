package co.codewizards.cloudstore.core.io;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class NoCloseInputStream extends FilterInputStream implements IInputStream {

	public NoCloseInputStream(InputStream in) {
		super(in);
	}

	@Override
	public void close() throws IOException {
		doNothing();
	}
}
