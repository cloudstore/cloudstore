package co.codewizards.cloudstore.core.dto.jaxb;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
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

	public void serialize(final D dto, final File file) {
		assertNotNull("dto", dto);
		assertNotNull("file", file);
		try {
			// Even though https://github.com/cloudstore/cloudstore/issues/31 seems to affect only unmarshal(File),
			// we manage the OutputStream ourself, as well.
			final OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
			try {
				getMarshaller().marshal(dto, out);
			} finally {
				out.close();
			}
		} catch (JAXBException | IOException e) {
			throw new RuntimeException("Writing file '" + file.getAbsolutePath() + "' failed: " + e, e);
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

	public D deserialize(final File file) {
		assertNotNull("file", file);
		try {
			// Because of https://github.com/cloudstore/cloudstore/issues/31 we do not use unmarshal(File), anymore.
			final InputStream in = new BufferedInputStream(new FileInputStream(file));
			try {
				return dtoClass.cast(getUnmarshaller().unmarshal(in));
			} finally {
				in.close();
			}
		} catch (JAXBException | IOException e) {
			throw new RuntimeException("Reading file '" + file.getAbsolutePath() + "' failed: " + e, e);
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
