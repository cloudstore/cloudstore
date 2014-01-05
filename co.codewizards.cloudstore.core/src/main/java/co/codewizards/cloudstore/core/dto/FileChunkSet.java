package co.codewizards.cloudstore.core.dto;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class FileChunkSet {

	private String path;
	private boolean fileExists = true;
	private String sha1;
	private Date lastModified;
	private long length;

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

	private List<FileChunk> fileChunks;

	public List<FileChunk> getFileChunks() {
		if (fileChunks == null)
			fileChunks = new ArrayList<FileChunk>();

		return fileChunks;
	}
	public void setFileChunks(List<FileChunk> fileChunks) {
		this.fileChunks = fileChunks;
	}

	public boolean isFileExists() {
		return fileExists;
	}
	public void setFileExists(boolean fileExists) {
		this.fileExists = fileExists;
	}
}
