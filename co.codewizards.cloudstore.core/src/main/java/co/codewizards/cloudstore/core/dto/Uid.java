package co.codewizards.cloudstore.core.dto;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;
import java.util.UUID;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import co.codewizards.cloudstore.core.dto.jaxb.UidXmlAdapter;
import co.codewizards.cloudstore.core.util.AssertUtil;
import co.codewizards.cloudstore.core.util.Base64Url;

/**
 * Universal identifier similar to a {@link UUID}.
 * <p>
 * The main difference is the encoding which is optimized for brevity. In contrast to the hex-encoding used
 * by {@code UUID}, the {@link #toString()} method uses standard
 * <a href="http://en.wikipedia.org/wiki/Base64">base64url</a>
 * (<a href="http://en.wikipedia.org/wiki/Base64#RFC_4648">RFC 4648</a>). The difference to normal base64 is
 * that base64url replaces '+' by '-' and '/' by '_' in order to make the encoded string usable in URLs
 * without any escaping.
 * <p>
 * <b>Important:</b> The string-representation is <b>case-sensitive</b>!
 * <p>
 * Examples showing the difference of {@code UUID} vs. {@code Uid}:
 * <pre> WQL8yMHUQ4FhZrB0cLux5g
 * WCRAGMeiz-2PPaKdmn-iww
 * Jd6_KRqpMivfuxXO4JmwtQ
 * 284tn0-92bIMRNV_4M53Tg
 * 8b726260-f9f3-439b-bf21-615bb4b6731d
 * 34fadc2b-5a58-4de8-b04c-6f315a6598cd
 * 15c3f6cb-6275-4557-b24c-2cd57cd07a6d
 * 11934d8c-d201-4a95-a714-e03ff48f5053
 * 46875d87-01ef-4ece-98cf-5a96a5946ef7</pre>
 * <p>
 * A string-encoded {@code UUID} always has a length 36 characters, while a {@code Uid} always has a length
 * of 22 characters. In other words, the strings are 38.89% shorter.
 * <p>
 * Instances of this class are immutable.
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
@XmlJavaTypeAdapter(type=Uid.class, value=UidXmlAdapter.class)
public class Uid implements Comparable<Uid> {
	private static final String CHARSET_ASCII = "ASCII";

	private final long hi;
	private final long lo;

	private static class RandomHolder {
		static final SecureRandom random = new SecureRandom();
		static final byte[] next16Bytes() {
			final byte[] bytes = new byte[16];
			random.nextBytes(bytes);
			return bytes;
		}
	}

	/**
	 * Creates a new random {@code Uid}.
	 */
	public Uid() {
		this(RandomHolder.next16Bytes());
	}

	/**
	 * Creates a new {@code Uid} with the given value.
	 * <p>
	 * This constructor is equivalent to {@link UUID#UUID(long, long) UUID(long, long)}.
	 * @param hi the most significant bits of the new {@code Uid}.
	 * @param lo the least significant bits of the new {@code Uid}.
	 */
	public Uid(final long hi, final long lo) {
		this.hi = hi;
		this.lo = lo;
	}

	public Uid(final byte[] bytes) {
		if (AssertUtil.assertNotNull("bytes", bytes).length != 16)
			throw new IllegalArgumentException("bytes.length != 16");

		long hi = 0;
		long lo = 0;

		for (int i = 0; i < Math.min(8, bytes.length); ++i)
			hi = (hi << 8) | (bytes[i] & 0xff);

		for (int i = 8; i < Math.min(16, bytes.length); ++i)
			lo = (lo << 8) | (bytes[i] & 0xff);

		this.hi = hi;
		this.lo = lo;
	}

	private static final String assertValidUidString(final String uidString) {
		if (AssertUtil.assertNotNull("uidString", uidString).length() != 22)
			throw new IllegalArgumentException("uidString.length != 22");

		return uidString;
	}

	/**
	 * Creates a new {@code Uid} instance from the encoded value in {@code uidString}.
	 * <p>
	 * This constructor is symmetric to the {@link #toString()} method: The output of {@code toString()} can
	 * be passed to this constructor to create a new instance with the same value as (and thus being
	 * {@linkplain #equals(Object) equal} to) the first instance.
	 *
	 * @param uidString the string-encoded value of the Uid.
	 * @see #toString()
	 */
	public Uid(final String uidString) {
		this(uidStringToByteArray(uidString));
	}

	private static byte[] uidStringToByteArray(final String uidString) {
		try {
			return Base64Url.decodeBase64(assertValidUidString(uidString).getBytes(CHARSET_ASCII));
		} catch (final UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	public byte[] toBytes() {
		final byte[] bytes = new byte[16];

		int idx = -1;
		for (int i = 7; i >= 0; --i)
			bytes[++idx] = (byte) (hi >> (8 * i));

		for (int i = 7; i >= 0; --i)
			bytes[++idx] = (byte) (lo >> (8 * i));

		return bytes;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (hi ^ (hi >>> 32));
		result = prime * result + (int) (lo ^ (lo >>> 32));
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final Uid other = (Uid) obj;
		if (hi != other.hi)
			return false;
		if (lo != other.lo)
			return false;
		return true;
	}

	/**
	 * Gets a base64url-encoded string-representation of this {@code Uid}.
	 * <p>
	 * The string returned by this method can be passed to {@link #Uid(String)} to create a new equal
	 * {@code Uid} instance.
	 * <p>
	 * <b>Important:</b> The string-representation is <b>case-sensitive</b>!
	 * <p>
	 * <b><u>Inherited documentation:</u></b><br/>
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		try {
			return new String(Base64Url.encodeBase64(toBytes()), CHARSET_ASCII);
		} catch (final UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public int compareTo(final Uid other) {
		AssertUtil.assertNotNull("other", other);
		// Same semantics as for normal numbers.
		return (this.hi < other.hi ? -1 :
				(this.hi > other.hi ? 1 :
				 (this.lo < other.lo ? -1 :
				  (this.lo > other.lo ? 1 :
				   0))));
	}
}
