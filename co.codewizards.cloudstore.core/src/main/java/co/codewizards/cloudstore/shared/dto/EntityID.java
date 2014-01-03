package co.codewizards.cloudstore.shared.dto;

import java.io.Serializable;
import java.util.UUID;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import co.codewizards.cloudstore.shared.dto.jaxb.EntityIDTypeAdapter;

/**
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
@XmlJavaTypeAdapter(type=EntityID.class, value=EntityIDTypeAdapter.class)
public class EntityID
implements Serializable
{
	private static final long serialVersionUID = 1L;

	public EntityID() { }

	public EntityID(final long idHigh, final long idLow) {
		this.idHigh = idHigh;
		this.idLow = idLow;
	}

	public EntityID(final UUID uuid) {
		idHigh = uuid.getMostSignificantBits();
		idLow = uuid.getLeastSignificantBits();
	}

	public EntityID(final String string) {
		this(UUID.fromString(string));
	}

	public long idHigh;
	public long idLow;

	public UUID toUUID()
	{
		final UUID uuid = new UUID(idHigh, idLow);
		return uuid;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (idHigh ^ (idHigh >>> 32));
		result = prime * result + (int) (idLow ^ (idLow >>> 32));
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}

		final EntityID other = (EntityID) obj;
		return idHigh == other.idHigh && idLow == other.idLow;
	}

	@Override
	public String toString() {
		return toUUID().toString();
	}
}