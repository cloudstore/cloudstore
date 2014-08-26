package co.codewizards.cloudstore.core.oio;

import java.io.RandomAccessFile;

/**
 * @author Sebastian Schefczyk
 */
public interface FileChannelFactory extends FileService {

	FileChannel createFileChannel(RandomAccessFile randomAccessFile);

}
