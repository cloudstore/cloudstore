package co.codewizards.cloudstore.shared.dto.jaxb;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import co.codewizards.cloudstore.shared.dto.ChangeSetRequest;
import co.codewizards.cloudstore.shared.dto.ChangeSetResponse;
import co.codewizards.cloudstore.shared.dto.DirectoryDTO;
import co.codewizards.cloudstore.shared.dto.EntityID;
import co.codewizards.cloudstore.shared.dto.Error;
import co.codewizards.cloudstore.shared.dto.ErrorStackTraceElement;
import co.codewizards.cloudstore.shared.dto.NormalFileDTO;
import co.codewizards.cloudstore.shared.dto.RepoFileDTO;
import co.codewizards.cloudstore.shared.dto.RepoFileDTOList;
import co.codewizards.cloudstore.shared.dto.RepositoryDTO;

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
