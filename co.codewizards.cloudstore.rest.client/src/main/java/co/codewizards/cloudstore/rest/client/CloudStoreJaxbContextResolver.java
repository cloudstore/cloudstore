package co.codewizards.cloudstore.rest.client;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBContext;

import co.codewizards.cloudstore.core.dto.jaxb.CloudStoreJaxbContext;

/**
 * {@link ContextResolver} implementation providing the {@link CloudStoreJaxbContext}.
 * <p>
 * Due to this {@link ContextResolver}, the REST client is able to serialise and deserialise all our DTOs
 * to and from XML.
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
@Provider
public class CloudStoreJaxbContextResolver implements ContextResolver<JAXBContext> {

	@Override
	public JAXBContext getContext(final Class<?> type) {
		return CloudStoreJaxbContext.getJaxbContext();
	}

}
