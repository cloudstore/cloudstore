package co.codewizards.cloudstore.shared.dto;

import java.io.File;
import java.util.Date;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class NormalFileDTO extends RepoFileDTO {

	private Date lastModified;

	private long length;

	private String sha1;

	/**
	 * Gets the timestamp of the file's last modification.
	 * <p>
	 * It reflects the {@link File#lastModified() File.lastModified} property.
	 * @return the timestamp of the file's last modification.
	 */
	public Date getLastModified() {
		return lastModified;
	}
	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}
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
}
