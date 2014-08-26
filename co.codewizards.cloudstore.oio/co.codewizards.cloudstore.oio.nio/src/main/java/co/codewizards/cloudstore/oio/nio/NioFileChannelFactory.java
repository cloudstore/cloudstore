package co.codewizards.cloudstore.oio.nio;

import java.io.RandomAccessFile;

import co.codewizards.cloudstore.core.oio.FileChannel;
import co.codewizards.cloudstore.core.oio.FileChannelFactory;
import co.codewizards.cloudstore.core.oio.FileService;

public class NioFileChannelFactory implements FileChannelFactory, FileService {

	@Override
	public int getPriority() {
		return 10;
	}

	@Override
	public FileChannel createFileChannel(final RandomAccessFile randomAccessFile) {
		return new NioFileChannel(randomAccessFile.getChannel());
	}

}
