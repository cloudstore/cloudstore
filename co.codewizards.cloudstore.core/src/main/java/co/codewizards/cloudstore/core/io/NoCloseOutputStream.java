package co.codewizards.cloudstore.core.io;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.DeflaterOutputStream;

public class NoCloseOutputStream extends FilterOutputStream implements IOutputStream {

	public NoCloseOutputStream(OutputStream out) {
		super(out);
	}

	@Override
	public void close() throws IOException {
		if (out instanceof DeflaterOutputStream)
			((DeflaterOutputStream) out).finish();

		flush();
	}
}
