package co.codewizards.cloudstore.core.auth;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import co.codewizards.cloudstore.core.util.AssertUtil;

public class AuthTokenIO {

	public byte[] serialise(AuthToken authToken) {
		AssertUtil.assertNotNull("authToken", authToken);
		try {
			JAXBContext context = createContext();
			Marshaller marshaller = context.createMarshaller();
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			marshaller.marshal(authToken, os);
			return os.toByteArray();
		} catch (JAXBException e) {
			throw new RuntimeException(e);
		}
	}

	public AuthToken deserialise(byte[] authTokenData) {
		AssertUtil.assertNotNull("authTokenData", authTokenData);
		try {
			JAXBContext context = createContext();
			Unmarshaller unmarshaller = context.createUnmarshaller();
			Object object = unmarshaller.unmarshal(new ByteArrayInputStream(authTokenData));
			return (AuthToken) object;
		} catch (JAXBException e) {
			throw new RuntimeException(e);
		}
	}

	private JAXBContext createContext() throws JAXBException {
		return JAXBContext.newInstance(AuthToken.class);
	}
}
