package co.codewizards.cloudstore.core.repo.transport.file;

import java.io.File;

class TempChunkFileWithDTOFile {

	private File tempChunkFile;
	private File tempChunkFileDTOFile;

	public File getTempChunkFile() {
		return tempChunkFile;
	}
	public void setTempChunkFile(File tempChunkFile) {
		this.tempChunkFile = tempChunkFile;
	}
	public File getTempChunkFileDTOFile() {
		return tempChunkFileDTOFile;
	}
	public void setTempChunkFileDTOFile(File tempChunkFileDTOFile) {
		this.tempChunkFileDTOFile = tempChunkFileDTOFile;
	}

}
