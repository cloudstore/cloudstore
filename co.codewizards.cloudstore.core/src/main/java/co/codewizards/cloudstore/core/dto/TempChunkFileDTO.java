package co.codewizards.cloudstore.core.dto;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class TempChunkFileDTO {
	private FileChunkDTO fileChunkDTO;

	public FileChunkDTO getFileChunkDTO() {
		return fileChunkDTO;
	}
	public void setFileChunkDTO(FileChunkDTO fileChunkDTO) {
		this.fileChunkDTO = fileChunkDTO;
	}
}
