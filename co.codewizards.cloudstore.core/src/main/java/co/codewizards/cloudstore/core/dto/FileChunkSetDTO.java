package co.codewizards.cloudstore.core.dto;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class FileChunkSetDTO {

	private String path;
	private boolean fileExists = true;
	private String sha1;
	private Date lastModified;
	private long length;

	private List<FileChunkDTO> fileChunkDTOs;

	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}

	public String getSha1() {
		return sha1;
	}
	public void setSha1(String sha1) {
		this.sha1 = sha1;
	}

	public long getLength() {
		return length;
	}
	public void setLength(long length) {
		this.length = length;
	}

	public Date getLastModified() {
		return lastModified;
	}
	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}

	public List<FileChunkDTO> getFileChunkDTOs() {
		if (fileChunkDTOs == null)
			fileChunkDTOs = new ArrayList<FileChunkDTO>();

		return fileChunkDTOs;
	}
	public void setFileChunkDTOs(List<FileChunkDTO> fileChunkDTOs) {
		this.fileChunkDTOs = fileChunkDTOs;
	}

	public boolean isFileExists() {
		return fileExists;
	}
	public void setFileExists(boolean fileExists) {
		this.fileExists = fileExists;
	}
}
