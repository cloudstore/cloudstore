package co.codewizards.cloudstore.local.transport;

import java.io.File;

public class TempChunkFileWithDTOFile {

	private File tempChunkFile;
	private File tempChunkFileDTOFile;

	public File getTempChunkFile() {
		return tempChunkFile;
	}
	public void setTempChunkFile(final File tempChunkFile) {
		this.tempChunkFile = tempChunkFile;
	}
	public File getTempChunkFileDTOFile() {
		return tempChunkFileDTOFile;
	}
	public void setTempChunkFileDTOFile(final File tempChunkFileDTOFile) {
		this.tempChunkFileDTOFile = tempChunkFileDTOFile;
	}

}
