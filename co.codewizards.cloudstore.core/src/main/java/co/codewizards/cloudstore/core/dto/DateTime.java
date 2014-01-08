package co.codewizards.cloudstore.core.dto;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import co.codewizards.cloudstore.core.util.Util;

public class DateTime {

	private final Date date;

	public DateTime(String dateString) {
		try {
			date = createDateFormat().parse(assertNotNull("dateString", dateString));
		} catch (ParseException e) {
			throw new IllegalArgumentException(e);
		}
	}

	private static DateFormat createDateFormat() {
		return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
	}

	public DateTime(Date date) {
		this.date = assertNotNull("date", date);
	}

	@Override
	public String toString() {
		return createDateFormat().format(date);
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

	public Date getDate() {
		return (Date) date.clone();
	}

	public long getMillis() {
		return date.getTime();
	}
}
