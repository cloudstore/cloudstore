package co.codewizards.cloudstore.core.dto;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.util.Date;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import co.codewizards.cloudstore.core.dto.jaxb.DateTimeXmlAdapter;
import co.codewizards.cloudstore.core.util.ISO8601;
import co.codewizards.cloudstore.core.util.Util;

@XmlJavaTypeAdapter(type=DateTime.class, value=DateTimeXmlAdapter.class)
public class DateTime {

	private final Date date;

	public DateTime(String dateString) {
		date = ISO8601.parseDate(assertNotNull("dateString", dateString));
	}

	public DateTime(Date date) {
		this.date = (Date) assertNotNull("date", date).clone();
	}

	public long getMillis() {
		return date.getTime();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((date == null) ? 0 : date.hashCode());
		return result;
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

	@Override
	public String toString() {
		return ISO8601.formatDate(date);
	}

	public Date toDate() {
		return (Date) date.clone();
	}
}
