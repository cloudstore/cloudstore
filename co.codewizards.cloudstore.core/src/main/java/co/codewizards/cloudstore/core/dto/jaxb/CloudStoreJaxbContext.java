package co.codewizards.cloudstore.core.dto.jaxb;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import co.codewizards.cloudstore.core.dto.ChangeSet;
import co.codewizards.cloudstore.core.dto.DeleteModificationDTO;
import co.codewizards.cloudstore.core.dto.DirectoryDTO;
import co.codewizards.cloudstore.core.dto.EntityID;
import co.codewizards.cloudstore.core.dto.Error;
import co.codewizards.cloudstore.core.dto.ErrorStackTraceElement;
import co.codewizards.cloudstore.core.dto.FileChunk;
import co.codewizards.cloudstore.core.dto.FileChunkSet;
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
						ChangeSet.class,
						DeleteModificationDTO.class,
						DirectoryDTO.class,
						EntityID.class,
						Error.class,
						ErrorStackTraceElement.class,
						FileChunkSet.class,
						FileChunk.class,
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
