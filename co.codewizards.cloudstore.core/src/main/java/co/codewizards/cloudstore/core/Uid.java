package co.codewizards.cloudstore.core;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.security.SecureRandom;
import java.util.UUID;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import co.codewizards.cloudstore.core.dto.jaxb.UidXmlAdapter;
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
public class Uid implements Comparable<Uid>, Serializable {
	/**
	 * Gets the length of an {@code Uid} in {@link #toBytes() bytes}.
	 */
	public static final int LENGTH_BYTES = 16;

	/**
	 * Gets the length of an {@code Uid} in its {@link #toString() String representation}.
	 */
	public static final int LENGTH_STRING = 22;

	private static final long serialVersionUID = 1L;

	private final long hi;
	private final long lo;
	private transient WeakReference<String> toString;

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

	/**
	 * Creates a new {@code Uid} instance from the binary data in {@code bytes}.
	 * <p>
	 * If the given {@code bytes} is <code>null</code> or empty (array-length 0), this method returns <code>null</code>.
	 * Otherwise, it creates an instance using the {@link #Uid(byte[])} constructor.
	 * @param bytes the raw binary data of the Uid. May be <code>null</code>. If not <code>null</code>,
	 * its length must be exactly 0 (treated the same a <code>null</code>) or {@value #LENGTH_BYTES}.
	 * @return an {@code Uid} instance created from the given {@code bytes} or <code>null</code>, if the input
	 * is <code>null</code> or empty (array-length 0).
	 */
	public static final Uid valueOf(final byte[] bytes) {
		return bytes == null || bytes.length == 0 ? null : new Uid(bytes);
	}

	public Uid(final byte[] bytes) {
		if (assertNotNull(bytes, "bytes").length != LENGTH_BYTES)
			throw new IllegalArgumentException("bytes.length != " + LENGTH_BYTES);

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
		if (assertNotNull(uidString, "uidString").length() != LENGTH_STRING)
			throw new IllegalArgumentException("uidString.length != " + LENGTH_STRING + " :: '" + uidString + "'");

		return uidString;
	}

	/**
	 * Creates a new {@code Uid} instance from the encoded value in {@code uidString}.
	 * <p>
	 * If the given {@code uidString} is <code>null</code> or empty, this method returns <code>null</code>.
	 * Otherwise, it creates an instance using the {@link #Uid(String)} constructor.
	 * @param uidString the string-encoded value of the Uid. May be <code>null</code>.
	 * @return an {@code Uid} instance created from the given {@code uidString} or <code>null</code>, if the input
	 * is <code>null</code> or empty.
	 */
	public static final Uid valueOf(final String uidString) {
		return uidString == null || uidString.isEmpty() ? null : new Uid(uidString);
	}

	/**
	 * Creates a new {@code Uid} instance from the encoded value in {@code uidString}.
	 * <p>
	 * This constructor is symmetric to the {@link #toString()} method: The output of {@code toString()} can
	 * be passed to this constructor to create a new instance with the same value as (and thus being
	 * {@linkplain #equals(Object) equal} to) the first instance.
	 *
	 * @param uidString the string-encoded value of the Uid. Must not be <code>null</code>.
	 * @see #toString()
	 */
	public Uid(final String uidString) {
		this(uidStringToByteArray(uidString));
	}

	private static byte[] uidStringToByteArray(final String uidString) {
		return Base64Url.decodeBase64FromString(assertValidUidString(uidString));
	}

	public byte[] toBytes() {
		final byte[] bytes = new byte[LENGTH_BYTES];

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
		String s = toString == null ? null : toString.get();
		if (s != null)
			return s;

		s = Base64Url.encodeBase64ToString(toBytes());

		if (s.length() != LENGTH_STRING) // sanity check
			throw new IllegalStateException("uidString.length != " + LENGTH_STRING);

		toString = new WeakReference<String>(s);
		return s;
	}

	@Override
	public int compareTo(final Uid other) {
		assertNotNull(other, "other");
		// Same semantics as for normal numbers.
		return (this.hi < other.hi ? -1 :
				(this.hi > other.hi ? 1 :
				 (this.lo < other.lo ? -1 :
				  (this.lo > other.lo ? 1 :
				   0))));
	}
}
