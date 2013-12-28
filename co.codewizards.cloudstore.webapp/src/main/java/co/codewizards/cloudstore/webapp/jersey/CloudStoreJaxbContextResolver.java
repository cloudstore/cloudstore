package co.codewizards.cloudstore.webapp.jersey;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBContext;

import co.codewizards.cloudstore.shared.dto.jaxb.CloudStoreJaxbContext;

@Provider
public class CloudStoreJaxbContextResolver implements ContextResolver<JAXBContext> {

	@Override
	public JAXBContext getContext(Class<?> type) {
		return CloudStoreJaxbContext.getJaxbContext();
	}

}
