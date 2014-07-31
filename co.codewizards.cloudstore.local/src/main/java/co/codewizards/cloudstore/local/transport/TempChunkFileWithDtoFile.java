package co.codewizards.cloudstore.local.transport;

import java.io.File;

public class TempChunkFileWithDtoFile {

	private File tempChunkFile;
	private File tempChunkFileDtoFile;

	public File getTempChunkFile() {
		return tempChunkFile;
	}
	public void setTempChunkFile(final File tempChunkFile) {
		this.tempChunkFile = tempChunkFile;
	}
	public File getTempChunkFileDtoFile() {
		return tempChunkFileDtoFile;
	}
	public void setTempChunkFileDtoFile(final File tempChunkFileDtoFile) {
		this.tempChunkFileDtoFile = tempChunkFileDtoFile;
	}

}
