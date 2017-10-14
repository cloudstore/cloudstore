package co.codewizards.cloudstore.core.dto;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class FileChunkDto {

	/**
	 * All chunks normally have the same length: 1 MiB. The last chunk, however, might be shorter, hence
	 * there's the {@link #getLength() length} property, too.
	 */
	public static final int MAX_LENGTH = 1024 * 1024;

	public FileChunkDto() {
	}

	private long offset;

	private int length;

	private String sha1;

	public long getOffset() {
		return offset;
	}
	public void setOffset(long offset) {
		this.offset = offset;
	}

	public int getLength() {
		return length;
	}
	public void setLength(int length) {
		this.length = length;
	}

	public String getSha1() {
		return sha1;
	}
	public void setSha1(String sha1) {
		this.sha1 = sha1;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + '[' + toString_getProperties() + ']';
	}

	protected String toString_getProperties() {
		return "offset=" + offset
				+ ", length=" + length
				+ ", sha1=" + sha1;
	}
}
