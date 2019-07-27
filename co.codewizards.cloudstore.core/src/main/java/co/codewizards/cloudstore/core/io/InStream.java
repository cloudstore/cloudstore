package co.codewizards.cloudstore.core.io;

import static java.util.Objects.*;

import java.io.IOException;
import java.io.InputStream;

class InStream extends InputStream implements IInputStream {

	protected final InputStream in;

	protected InStream() {
		in = null;
	}

	protected InStream(final InputStream in) {
		this.in = requireNonNull(in, "in");
	}

	@Override
	public int read() throws IOException {
		return in.read();
	}

	@Override
	public int read(byte[] b) throws IOException {
		return in.read(b);
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		return in.read(b, off, len);
	}

	@Override
	public long skip(long n) throws IOException {
		return in.skip(n);
	}

	@Override
	public int available() throws IOException {
		return in.available();
	}

	@Override
	public void close() throws IOException {
		in.close();
	}

	@Override
	public synchronized void mark(int readlimit) {
		in.mark(readlimit);
	}

	@Override
	public synchronized void reset() throws IOException {
		in.reset();
	}

	@Override
	public boolean markSupported() {
		return in.markSupported();
	}


	public static class InverseInStream extends InStream {

		protected final IInputStream in;

		protected InverseInStream(final IInputStream in) {
			this.in = requireNonNull(in, "in");
		}

		@Override
		public int read() throws IOException {
			return in.read();
		}

		@Override
		public int read(byte[] b) throws IOException {
			return in.read(b);
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			return in.read(b, off, len);
		}

		@Override
		public long skip(long n) throws IOException {
			return in.skip(n);
		}

		@Override
		public int available() throws IOException {
			return in.available();
		}

		@Override
		public void close() throws IOException {
			in.close();
		}

		@Override
		public synchronized void mark(int readlimit) {
			in.mark(readlimit);
		}

		@Override
		public synchronized void reset() throws IOException {
			in.reset();
		}

		@Override
		public boolean markSupported() {
			return in.markSupported();
		}
	}
}
