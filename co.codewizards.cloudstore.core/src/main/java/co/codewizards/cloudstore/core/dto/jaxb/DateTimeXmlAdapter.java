package co.codewizards.cloudstore.core.dto.jaxb;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import co.codewizards.cloudstore.core.dto.DateTime;

public class DateTimeXmlAdapter extends XmlAdapter<String, DateTime> {

	@Override
	public DateTime unmarshal(final String v) throws Exception {
		return new DateTime(v);
	}

	@Override
	public String marshal(final DateTime v) throws Exception {
		return v.toString();
	}

}
