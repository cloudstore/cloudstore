package co.codewizards.cloudstore.core.dto.jaxb;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import co.codewizards.cloudstore.core.Uid;

public class UidXmlAdapter extends XmlAdapter<String, Uid> {

	@Override
	public Uid unmarshal(final String v) throws Exception {
		return new Uid(v);
	}

	@Override
	public String marshal(final Uid v) throws Exception {
		return v.toString();
	}

}
