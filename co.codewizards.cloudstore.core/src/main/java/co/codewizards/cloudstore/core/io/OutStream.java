package co.codewizards.cloudstore.core.io;

import static java.util.Objects.*;

import java.io.IOException;
import java.io.OutputStream;

class OutStream extends OutputStream implements IOutputStream {

	protected final OutputStream out;

	protected OutStream() {
		out = null;
	}

	protected OutStream(final OutputStream out) {
		this.out = requireNonNull(out, "out");
	}

	@Override
	public void write(int b) throws IOException {
		out.write(b);
	}

	@Override
	public void write(byte[] b) throws IOException {
		out.write(b);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		out.write(b, off, len);
	}

	@Override
	public void flush() throws IOException {
		out.flush();
	}

	@Override
	public void close() throws IOException {
		out.close();
	}


	public static class InverseOutStream extends OutStream {

		protected final IOutputStream out;

		protected InverseOutStream(final IOutputStream out) {
			this.out = requireNonNull(out, "out");
		}

		@Override
		public void write(int b) throws IOException {
			out.write(b);
		}

		@Override
		public void write(byte[] b) throws IOException {
			out.write(b);
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			out.write(b, off, len);
		}

		@Override
		public void flush() throws IOException {
			out.flush();
		}

		@Override
		public void close() throws IOException {
			out.close();
		}
	}
}
