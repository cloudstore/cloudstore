package co.codewizards.cloudstore.core.dto.jaxb;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

public abstract class DTOIO <D> {

	private final Class<D> dtoClass;

	private Marshaller marshaller;
	private Unmarshaller unmarshaller;

	protected DTOIO() {
		ParameterizedType superclass = (ParameterizedType) getClass().getGenericSuperclass();
		Type[] actualTypeArguments = superclass.getActualTypeArguments();
		if (actualTypeArguments == null || actualTypeArguments.length < 1)
			throw new IllegalStateException("Subclass " + getClass().getName() + " has no generic type argument!");

		@SuppressWarnings("unchecked")
		Class<D> c = (Class<D>) actualTypeArguments[0];
		this.dtoClass = c;
		if (this.dtoClass == null)
			throw new IllegalStateException("Subclass " + getClass().getName() + " has no generic type argument!");
	}

	public void serialize(D dto, OutputStream out) {
		assertNotNull("dto", dto);
		assertNotNull("out", out);
		try {
			getMarshaller().marshal(dto, out);
		} catch (JAXBException e) {
			throw new RuntimeException(e);
		}
	}

	public void serialize(D dto, File out) {
		assertNotNull("dto", dto);
		assertNotNull("out", out);
		try {
			getMarshaller().marshal(dto, out);
		} catch (JAXBException e) {
			throw new RuntimeException(e);
		}
	}

	public D deserialize(InputStream in) {
		assertNotNull("in", in);
		try {
			return dtoClass.cast(getUnmarshaller().unmarshal(in));
		} catch (JAXBException e) {
			throw new RuntimeException(e);
		}
	}

	public D deserialize(File in) {
		assertNotNull("in", in);
		try {
			return dtoClass.cast(getUnmarshaller().unmarshal(in));
		} catch (JAXBException e) {
			throw new RuntimeException("Reading file '" + in.getAbsolutePath() + "' failed: " + e, e);
		}
	}

	private Marshaller getMarshaller() {
		if (marshaller == null) {
			try {
				marshaller = CloudStoreJaxbContext.getJaxbContext().createMarshaller();
			} catch (JAXBException e) {
				throw new RuntimeException(e);
			}
		}
		return marshaller;
	}

	private Unmarshaller getUnmarshaller() {
		if (unmarshaller == null) {
			try {
				unmarshaller = CloudStoreJaxbContext.getJaxbContext().createUnmarshaller();
			} catch (JAXBException e) {
				throw new RuntimeException(e);
			}
		}
		return unmarshaller;
	}

}
