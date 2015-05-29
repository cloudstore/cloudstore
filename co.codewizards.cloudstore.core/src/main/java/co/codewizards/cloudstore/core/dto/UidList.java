package co.codewizards.cloudstore.core.dto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class UidList extends ArrayList<Uid> {
	private static final long serialVersionUID = 1L;

	public UidList() {
	}

	public UidList(int initialCapacity) {
		super(initialCapacity);
	}

	public UidList(Collection<? extends Uid> c) {
		super(c);
	}

	public UidList(final String pgpKeyIdsString) {
		if (pgpKeyIdsString == null)
			return;

		final StringTokenizer st = new StringTokenizer(pgpKeyIdsString, ", \t", false);
		while (st.hasMoreTokens()) {
			final String token = st.nextToken();
			if (!token.isEmpty())
				this.add(new Uid(token));
		}
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		for (final Uid uid : this) {
			if (sb.length() > 0)
				sb.append(',');

			sb.append(uid);
		}
		return sb.toString();
	}

	/**
	 * Gets the elements of this list.
	 * @return {@code this}
	 * @deprecated This method should not be invoked by manually written code! It is exclusively used by JAXB.
	 */
	@Deprecated
	@XmlElement(name="uid")
	public List<Uid> getElements() {
		return this;
	}
}
