package co.codewizards.cloudstore.core.dto.jaxb;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import co.codewizards.cloudstore.core.version.Version;

public class VersionXmlAdapter extends XmlAdapter<String, Version> {

	@Override
	public Version unmarshal(final String v) throws Exception {
		return new Version(v);
	}

	@Override
	public String marshal(final Version v) throws Exception {
		return v.toString();
	}

}
