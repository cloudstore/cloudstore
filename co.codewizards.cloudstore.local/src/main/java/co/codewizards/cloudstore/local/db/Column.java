package co.codewizards.cloudstore.local.db;

import static java.util.Objects.*;

import java.util.Objects;

public class Column implements Comparable<Column> {

	public final Table table;
	public final String name;
	public final int dataType;
	public final int size;
	public final Boolean autoIncrement;

	public Column(Table table, String name, int dataType, int size, Boolean autoIncrement) {
		this.table = requireNonNull(table, "table");
		this.name = requireNonNull(name, "name");
		this.dataType = dataType;
		this.size = size;
		this.autoIncrement = autoIncrement;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[table=" + table
				+ ", name=" + toStringWithSingleQuotes(name)
				+ ", dataType=" + dataType
				+ ", size=" + size
				+ "]";
	}

	private static String toStringWithSingleQuotes(String value) {
		if (value == null)
			return String.valueOf(value);

		return "'" + value + "'";
	}

	@Override
	public int compareTo(Column other) {
		int res = this.table.compareTo(other.table);
		if (res != 0)
			return res;

		res = compare(this.name, other.name);
		return res;
	}

	private static int compare(String s1, String s2) {
		if (s1 == null) {
			if (s2 == null)
				return 0;
			else
				return -1;
		}
		if (s2 == null)
			return +1;

		return s1.compareTo(s2);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((table == null) ? 0 : table.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		Column other = (Column) obj;
		return Objects.equals(this.name, other.name)
				&& this.table.equals(other.table);
	}
}
