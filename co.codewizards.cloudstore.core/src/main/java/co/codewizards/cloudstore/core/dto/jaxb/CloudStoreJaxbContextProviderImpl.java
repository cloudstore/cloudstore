package co.codewizards.cloudstore.core.dto.jaxb;

import co.codewizards.cloudstore.core.auth.EncryptedSignedAuthToken;
import co.codewizards.cloudstore.core.dto.ChangeSetDto;
import co.codewizards.cloudstore.core.dto.CopyModificationDto;
import co.codewizards.cloudstore.core.dto.DeleteModificationDto;
import co.codewizards.cloudstore.core.dto.DirectoryDto;
import co.codewizards.cloudstore.core.dto.Error;
import co.codewizards.cloudstore.core.dto.ErrorStackTraceElement;
import co.codewizards.cloudstore.core.dto.FileChunkDto;
import co.codewizards.cloudstore.core.dto.ListDto;
import co.codewizards.cloudstore.core.dto.ModificationDto;
import co.codewizards.cloudstore.core.dto.NormalFileDto;
import co.codewizards.cloudstore.core.dto.RepoFileDto;
import co.codewizards.cloudstore.core.dto.RepositoryDto;
import co.codewizards.cloudstore.core.dto.SymlinkDto;
import co.codewizards.cloudstore.core.dto.TempChunkFileDto;

public class CloudStoreJaxbContextProviderImpl extends AbstractCloudStoreJaxbContextProvider {

	@Override
	public Class<?>[] getClassesToBeBound() {
		return new Class<?>[] {
				ChangeSetDto.class,
				CopyModificationDto.class,
				DeleteModificationDto.class,
				DirectoryDto.class,
				EncryptedSignedAuthToken.class,
				Error.class,
				ErrorStackTraceElement.class,
				FileChunkDto.class,
				ListDto.class,
				ModificationDto.class,
				NormalFileDto.class,
				RepoFileDto.class,
				RepositoryDto.class,
				SymlinkDto.class,
				TempChunkFileDto.class
		};
	}

}
