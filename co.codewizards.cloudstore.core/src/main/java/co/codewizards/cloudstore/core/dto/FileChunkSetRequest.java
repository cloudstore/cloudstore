package co.codewizards.cloudstore.core.dto;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class FileChunkSetRequest {

	private String path;
//	private int chunkSize;

	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}

}
