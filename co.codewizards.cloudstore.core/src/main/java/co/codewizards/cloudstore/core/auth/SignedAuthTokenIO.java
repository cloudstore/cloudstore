package co.codewizards.cloudstore.core.auth;

import static java.util.Objects.*;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import co.codewizards.cloudstore.core.io.ByteArrayInputStream;
import co.codewizards.cloudstore.core.io.ByteArrayOutputStream;

public class SignedAuthTokenIO {
	public byte[] serialise(SignedAuthToken signedAuthToken) {
		requireNonNull(signedAuthToken, "signedAuthToken");
		try {
			JAXBContext context = createContext();
			Marshaller marshaller = context.createMarshaller();
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			marshaller.marshal(signedAuthToken, os);
			return os.toByteArray();
		} catch (JAXBException e) {
			throw new RuntimeException(e);
		}
	}

	public SignedAuthToken deserialise(byte[] signedAuthTokenData) {
		requireNonNull(signedAuthTokenData, "signedAuthTokenData");
		try {
			JAXBContext context = createContext();
			Unmarshaller unmarshaller = context.createUnmarshaller();
			Object object = unmarshaller.unmarshal(new ByteArrayInputStream(signedAuthTokenData));
			return (SignedAuthToken) object;
		} catch (JAXBException e) {
			throw new RuntimeException(e);
		}
	}

	private JAXBContext createContext() throws JAXBException {
		return JAXBContext.newInstance(SignedAuthToken.class);
	}
}
