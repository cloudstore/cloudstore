package co.codewizards.cloudstore.core.dto.jaxb;

import java.util.UUID;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import co.codewizards.cloudstore.core.dto.EntityID;

/**
 * @author sschefczyk
 */
public class EntityIDTypeAdapter extends XmlAdapter<UUID, EntityID> {

	@Override
	public EntityID unmarshal(final UUID v) throws Exception {
		return new EntityID(v);
	}

	@Override
	public UUID marshal(final EntityID v) throws Exception {
		return v.toUUID();
	}

}
