package co.codewizards.cloudstore.core.dto.jaxb;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import co.codewizards.cloudstore.core.dto.ChangeSetRequest;
import co.codewizards.cloudstore.core.dto.ChangeSetResponse;
import co.codewizards.cloudstore.core.dto.DirectoryDTO;
import co.codewizards.cloudstore.core.dto.EntityID;
import co.codewizards.cloudstore.core.dto.Error;
import co.codewizards.cloudstore.core.dto.ErrorStackTraceElement;
import co.codewizards.cloudstore.core.dto.NormalFileDTO;
import co.codewizards.cloudstore.core.dto.RepoFileDTO;
import co.codewizards.cloudstore.core.dto.RepoFileDTOList;
import co.codewizards.cloudstore.core.dto.RepositoryDTO;

public class CloudStoreJaxbContext {

	private static class JaxbContextHolder {
		private static final JAXBContext jaxbContext;
		static {
			try {
				jaxbContext = JAXBContext.newInstance(
						ChangeSetRequest.class,
						ChangeSetResponse.class,
						DirectoryDTO.class,
						EntityID.class,
						Error.class,
						ErrorStackTraceElement.class,
						NormalFileDTO.class,
						RepoFileDTO.class,
						RepoFileDTOList.class,
						RepositoryDTO.class
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
