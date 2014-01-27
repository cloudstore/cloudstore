package co.codewizards.cloudstore.core.dto.jaxb;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import co.codewizards.cloudstore.core.auth.EncryptedSignedAuthToken;
import co.codewizards.cloudstore.core.dto.ChangeSetDTO;
import co.codewizards.cloudstore.core.dto.DeleteModificationDTO;
import co.codewizards.cloudstore.core.dto.DirectoryDTO;
import co.codewizards.cloudstore.core.dto.EntityID;
import co.codewizards.cloudstore.core.dto.Error;
import co.codewizards.cloudstore.core.dto.ErrorStackTraceElement;
import co.codewizards.cloudstore.core.dto.FileChunkDTO;
import co.codewizards.cloudstore.core.dto.ModificationDTO;
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
						ChangeSetDTO.class,
						DeleteModificationDTO.class,
						DirectoryDTO.class,
						EncryptedSignedAuthToken.class,
						EntityID.class,
						Error.class,
						ErrorStackTraceElement.class,
						FileChunkDTO.class,
						ModificationDTO.class,
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
