package co.codewizards.cloudstore.core.dto.jaxb;

import co.codewizards.cloudstore.core.auth.EncryptedSignedAuthToken;
import co.codewizards.cloudstore.core.dto.ChangeSetDTO;
import co.codewizards.cloudstore.core.dto.CopyModificationDTO;
import co.codewizards.cloudstore.core.dto.DeleteModificationDTO;
import co.codewizards.cloudstore.core.dto.DirectoryDTO;
import co.codewizards.cloudstore.core.dto.ErrorStackTraceElement;
import co.codewizards.cloudstore.core.dto.FileChunkDTO;
import co.codewizards.cloudstore.core.dto.ModificationDTO;
import co.codewizards.cloudstore.core.dto.NormalFileDTO;
import co.codewizards.cloudstore.core.dto.RepoFileDTO;
import co.codewizards.cloudstore.core.dto.RepoFileDTOList;
import co.codewizards.cloudstore.core.dto.RepositoryDTO;
import co.codewizards.cloudstore.core.dto.SymlinkDTO;
import co.codewizards.cloudstore.core.dto.TempChunkFileDTO;

public class CloudStoreJaxbContextProviderImpl extends AbstractCloudStoreJaxbContextProvider {

	@Override
	public Class<?>[] getClassesToBeBound() {
		return new Class<?>[] {
				ChangeSetDTO.class,
				CopyModificationDTO.class,
				DeleteModificationDTO.class,
				DirectoryDTO.class,
				EncryptedSignedAuthToken.class,
				Error.class,
				ErrorStackTraceElement.class,
				FileChunkDTO.class,
				ModificationDTO.class,
				NormalFileDTO.class,
				RepoFileDTO.class,
				RepoFileDTOList.class,
				RepositoryDTO.class,
				SymlinkDTO.class,
				TempChunkFileDTO.class
		};
	}

}
