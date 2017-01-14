package co.codewizards.cloudstore.core.dto;

import static co.codewizards.cloudstore.core.util.AssertUtil.assertNotNull;

import java.util.Date;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import co.codewizards.cloudstore.core.dto.jaxb.DateTimeXmlAdapter;
import co.codewizards.cloudstore.core.util.ISO8601;
import co.codewizards.cloudstore.core.util.Util;

/**
 * Immutable representation of a timestamp (a date and a time).
 * <p>
 * This object serves as a Dto both inside XML and in URLs (usually as
 * a query parameter, but it may be used inside a path, too). For this
 * purpose, its {@link #toString()} method and its {@linkplain #DateTime(String) single-String-argument-constructor}
 * are used.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
@XmlJavaTypeAdapter(type=DateTime.class, value=DateTimeXmlAdapter.class)
public class DateTime {

	private final Date date;

	/**
	 * Creates a new instance from the given ISO-8601-encoded {@code dateString}.
	 * <p>
	 * The result of the {@link #toString()} method can be passed to this constructor
	 * to obtain a copy of the original {@code DateTime} instance. This feature should
	 * be used to transport ISO-8601-encoded timestamps over the network:
	 * <p>
	 * <pre> DateTime original = new DateTime(new Date());
	 * String iso8601encoded = original.toString();
	 * // iso8601encoded could now be transported to a remote machine as part of a
	 * // REST URL (e.g. as query parameter).
	 *
	 * // The remote machine might then decode it using this REST-conform constructor:
	 * DateTime copy = new DateTime(iso8601encoded);</pre>
	 * <p>
	 * Because of this constructor, {@code DateTime} parameters can be directly used in
	 * REST resource (a.k.a. service) methods.
	 * @param dateString the ISO-8601-encoded form of a timestamp. Must not be <code>null</code>.
	 * @see #toString()
	 */
	public DateTime(String dateString) {
		date = ISO8601.parseDate(assertNotNull(dateString, "dateString"));
	}

	/**
	 * Creates a new instance from the given {@code date}.
	 * <p>
	 * Because {@link Date} instances are mutable, the given {@code date} is cloned.
	 * This way it can be guaranteed that {@code DateTime} instances are immutable.
	 * @param date the date to be cloned and wrapped in the new {@code DateTime} instance. Must not be <code>null</code>.
	 */
	public DateTime(Date date) {
		this.date = (Date) assertNotNull(date, "date").clone();
	}

	/**
	 * Gets the number of milliseconds since 1970 January 1 00:00:00 GMT represented by this {@code DateTime} object.
	 * @return the number of milliseconds since 1970 January 1 00:00:00 GMT represented by this {@code DateTime} object.
	 * @see Date#getTime()
	 */
	public long getMillis() {
		return date.getTime();
	}

	@Override
	public int hashCode() {
		return date == null ? 0 : date.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DateTime other = (DateTime) obj;
		return Util.equal(this.date, other.date);
	}

	/**
	 * Returns an ISO-8601-encoded timestamp which can be passed to {@link #DateTime(String)}.
	 * <p>
	 * <b><u>Inherited javadoc:</u></b>
	 * <p>
	 * {@inheritDoc}
	 * @see #DateTime(String)
	 */
	@Override
	public String toString() {
		return ISO8601.formatDate(date);
	}

	/**
	 * Converts this {@code DateTime} into a new {@link Date} instance.
	 * @return a new {@link Date} instance representing the same timestamp as this {@code DateTime}. Never <code>null</code>.
	 */
	public Date toDate() {
		return (Date) date.clone();
	}
}
