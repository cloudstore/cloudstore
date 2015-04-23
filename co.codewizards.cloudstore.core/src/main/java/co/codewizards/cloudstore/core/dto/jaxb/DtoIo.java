package co.codewizards.cloudstore.core.dto.jaxb;

import static co.codewizards.cloudstore.core.util.AssertUtil.assertNotNull;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import co.codewizards.cloudstore.core.io.NoCloseInputStream;
import co.codewizards.cloudstore.core.io.NoCloseOutputStream;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.util.AssertUtil;

public abstract class DtoIo <D> {

	private final Class<D> dtoClass;

	private Marshaller marshaller;
	private Unmarshaller unmarshaller;

	protected DtoIo() {
		final ParameterizedType superclass = (ParameterizedType) getClass().getGenericSuperclass();
		final Type[] actualTypeArguments = superclass.getActualTypeArguments();
		if (actualTypeArguments == null || actualTypeArguments.length < 1)
			throw new IllegalStateException("Subclass " + getClass().getName() + " has no generic type argument!");

		@SuppressWarnings("unchecked")
		final Class<D> c = (Class<D>) actualTypeArguments[0];
		this.dtoClass = c;
		if (this.dtoClass == null)
			throw new IllegalStateException("Subclass " + getClass().getName() + " has no generic type argument!");
	}

	public void serialize(final D dto, final OutputStream out) {
		assertNotNull("dto", dto);
		assertNotNull("out", out);
		try {
			getMarshaller().marshal(dto, new NoCloseOutputStream(out));
		} catch (final JAXBException e) {
			throw new RuntimeException(e);
		}
	}

	public void serializeWithGz(final D dto, final OutputStream out) {
		assertNotNull("dto", dto);
		assertNotNull("out", out);
		try {
			try (GZIPOutputStream gzOut = new GZIPOutputStream(new NoCloseOutputStream(out));) {
				getMarshaller().marshal(dto, gzOut);
			}
		} catch (JAXBException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void serialize(final D dto, final File file) {
		assertNotNull("dto", dto);
		assertNotNull("file", file);
		try {
			// Even though https://github.com/cloudstore/cloudstore/issues/31 seems to affect only unmarshal(File),
			// we manage the OutputStream ourself, as well.
			try (final OutputStream out = new BufferedOutputStream(file.createOutputStream());) {
				getMarshaller().marshal(dto, out);
			}
		} catch (JAXBException | IOException e) {
			throw new RuntimeException("Writing file '" + file.getAbsolutePath() + "' failed: " + e, e);
		}
	}

	public void serializeWithGz(final D dto, final File file) {
		assertNotNull("dto", dto);
		assertNotNull("file", file);
		try {
			// Even though https://github.com/cloudstore/cloudstore/issues/31 seems to affect only unmarshal(File),
			// we manage the OutputStream ourself, as well.
			try (final OutputStream out = new BufferedOutputStream(file.createOutputStream());) {
				try (GZIPOutputStream gzOut = new GZIPOutputStream(out);) {
					getMarshaller().marshal(dto, gzOut);
				}
			}
		} catch (JAXBException | IOException e) {
			throw new RuntimeException("Writing file '" + file.getAbsolutePath() + "' failed: " + e, e);
		}
	}

	public D deserialize(final InputStream in) {
		assertNotNull("in", in);
		try {
			return dtoClass.cast(getUnmarshaller().unmarshal(new NoCloseInputStream(in)));
		} catch (final JAXBException e) {
			throw new RuntimeException(e);
		}
	}

	public D deserializeWithGz(final InputStream in) {
		assertNotNull("in", in);
		try {
			try (GZIPInputStream gzIn = new GZIPInputStream(new NoCloseInputStream(in));) {
				return dtoClass.cast(getUnmarshaller().unmarshal(gzIn));
			}
		} catch (JAXBException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	public D deserialize(final File file) {
		AssertUtil.assertNotNull("file", file);
		try {
			// Because of https://github.com/cloudstore/cloudstore/issues/31 we do not use unmarshal(File), anymore.
			try (final InputStream in = new BufferedInputStream(file.createInputStream());) {
				return dtoClass.cast(getUnmarshaller().unmarshal(in));
			}
		} catch (JAXBException | IOException e) {
			throw new RuntimeException("Reading file '" + file.getAbsolutePath() + "' failed: " + e, e);
		}
	}

	public D deserializeWithGz(final File file) {
		AssertUtil.assertNotNull("file", file);
		try {
			// Because of https://github.com/cloudstore/cloudstore/issues/31 we do not use unmarshal(File), anymore.
			try (final InputStream in = new BufferedInputStream(file.createInputStream());) {
				try (GZIPInputStream gzIn = new GZIPInputStream(in);) {
					return dtoClass.cast(getUnmarshaller().unmarshal(gzIn));
				}
			}
		} catch (JAXBException | IOException e) {
			throw new RuntimeException("Reading file '" + file.getAbsolutePath() + "' failed: " + e, e);
		}
	}

	private Marshaller getMarshaller() {
		if (marshaller == null) {
			try {
				marshaller = CloudStoreJaxbContext.getJaxbContext().createMarshaller();
			} catch (final JAXBException e) {
				throw new RuntimeException(e);
			}
		}
		return marshaller;
	}

	private Unmarshaller getUnmarshaller() {
		if (unmarshaller == null) {
			try {
				unmarshaller = CloudStoreJaxbContext.getJaxbContext().createUnmarshaller();
			} catch (final JAXBException e) {
				throw new RuntimeException(e);
			}
		}
		return unmarshaller;
	}
}
