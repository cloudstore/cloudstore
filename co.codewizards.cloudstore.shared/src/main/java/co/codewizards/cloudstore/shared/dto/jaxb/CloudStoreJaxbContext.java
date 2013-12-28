package co.codewizards.cloudstore.shared.dto.jaxb;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import co.codewizards.cloudstore.shared.dto.Error;
import co.codewizards.cloudstore.shared.dto.ErrorStackTraceElement;
import co.codewizards.cloudstore.shared.dto.RepoFileDTO;

public class CloudStoreJaxbContext {

	private static class JaxbContextHolder {
		private static final JAXBContext jaxbContext;
		static {
			try {
				jaxbContext = JAXBContext.newInstance(
						Error.class,
						ErrorStackTraceElement.class,
						RepoFileDTO.class
						);
			} catch (JAXBException x) {
				throw new RuntimeException(x);
			}
		}
	}

	public static JAXBContext getJaxbContext() {
		return JaxbContextHolder.jaxbContext;
	}
}
