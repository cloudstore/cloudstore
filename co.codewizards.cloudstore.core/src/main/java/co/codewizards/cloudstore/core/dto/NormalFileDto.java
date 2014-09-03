package co.codewizards.cloudstore.core.dto;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import co.codewizards.cloudstore.core.oio.File;

@XmlRootElement
public class NormalFileDto extends RepoFileDto {

	private long length;

	private String sha1;

	private List<FileChunkDto> fileChunkDtos;

	private List<FileChunkDto> tempFileChunkDtos;

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

	public List<FileChunkDto> getFileChunkDtos() {
		if (fileChunkDtos == null)
			fileChunkDtos = new ArrayList<FileChunkDto>();

		return fileChunkDtos;
	}
	public void setFileChunkDtos(List<FileChunkDto> fileChunkDtos) {
		this.fileChunkDtos = fileChunkDtos;
	}

	public List<FileChunkDto> getTempFileChunkDtos() {
		if (tempFileChunkDtos == null)
			tempFileChunkDtos = new ArrayList<FileChunkDto>();

		return tempFileChunkDtos;
	}
	public void setTempFileChunkDtos(List<FileChunkDto> tempFileChunkDtos) {
		this.tempFileChunkDtos = tempFileChunkDtos;
	}
}
