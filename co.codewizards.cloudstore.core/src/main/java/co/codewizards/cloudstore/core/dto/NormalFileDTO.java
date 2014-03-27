package co.codewizards.cloudstore.core.dto;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class NormalFileDTO extends RepoFileDTO {

	private long length;

	private String sha1;

	private List<FileChunkDTO> fileChunkDTOs;

	private List<FileChunkDTO> tempFileChunkDTOs;

	/**
	 * Gets the file size in bytes.
	 * <p>
	 * It reflects the {@link File#length() File.length} property.
	 * @return the file size in bytes. <code>0</code>, if this is a directory.
	 */
	public long getLength() {
		return length;
	}
	public void setLength(long size) {
		this.length = size;
	}
	/**
	 * Gets the <a href="http://en.wikipedia.org/wiki/SHA-1">SHA-1</a> of the file.
	 * @return the <a href="http://en.wikipedia.org/wiki/SHA-1">SHA-1</a> of the file.
	 */
	public String getSha1() {
		return sha1;
	}
	public void setSha1(String sha) {
		this.sha1 = sha;
	}

	public List<FileChunkDTO> getFileChunkDTOs() {
		if (fileChunkDTOs == null)
			fileChunkDTOs = new ArrayList<FileChunkDTO>();

		return fileChunkDTOs;
	}
	public void setFileChunkDTOs(List<FileChunkDTO> fileChunkDTOs) {
		this.fileChunkDTOs = fileChunkDTOs;
	}

	public List<FileChunkDTO> getTempFileChunkDTOs() {
		if (tempFileChunkDTOs == null)
			tempFileChunkDTOs = new ArrayList<FileChunkDTO>();

		return tempFileChunkDTOs;
	}
	public void setTempFileChunkDTOs(List<FileChunkDTO> tempFileChunkDTOs) {
		this.tempFileChunkDTOs = tempFileChunkDTOs;
	}
}
