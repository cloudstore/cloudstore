package co.codewizards.cloudstore.local.db;

import static java.util.Objects.*;

import java.util.Objects;

public class Table implements Comparable<Table> {

	public final String catalogue;
	public final String schema;
	public final String name;

	public Table(String catalogue, String schema, String name) {
		this.catalogue = catalogue;
		this.schema = schema;
		this.name = requireNonNull(name, "name");
	}

	@Override
	public String toString() {
		return getClass().getSimpleName()
				+ "[catalogue=" + toStringWithSingleQuotes(catalogue)
				+ ", schema=" + toStringWithSingleQuotes(schema)
				+ ", name=" + toStringWithSingleQuotes(name)
				+ "]";
	}

	private static String toStringWithSingleQuotes(String value) {
		if (value == null)
			return String.valueOf(value);

		return "'" + value + "'";
	}

	@Override
	public int compareTo(Table other) {
		requireNonNull(other, "other");
		int res = compare(this.catalogue, other.catalogue);
		if (res != 0)
			return res;

		res = compare(this.schema, other.schema);
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
		result = prime * result + ((catalogue == null) ? 0 : catalogue.hashCode());
		result = prime * result + ((schema == null) ? 0 : schema.hashCode());
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
		Table other = (Table) obj;
		return Objects.equals(this.name, other.name)
				&& Objects.equals(this.schema, other.schema)
				&& Objects.equals(this.catalogue, other.catalogue);
	}
}
