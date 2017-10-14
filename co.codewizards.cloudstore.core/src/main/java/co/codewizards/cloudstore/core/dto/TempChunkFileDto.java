package co.codewizards.cloudstore.core.dto;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class TempChunkFileDto {
	private FileChunkDto fileChunkDto;

	public FileChunkDto getFileChunkDto() {
		return fileChunkDto;
	}
	public void setFileChunkDto(FileChunkDto fileChunkDto) {
		this.fileChunkDto = fileChunkDto;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + '[' + toString_getProperties() + ']';
	}

	protected String toString_getProperties() {
		return "fileChunkDto=" + fileChunkDto;
	}
}
