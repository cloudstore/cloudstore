package co.codewizards.cloudstore.ls.core.dto.jaxb;

import co.codewizards.cloudstore.core.dto.jaxb.AbstractCloudStoreJaxbContextProvider;
import co.codewizards.cloudstore.ls.core.dto.RemoteRepositoryDto;
import co.codewizards.cloudstore.ls.core.dto.RepoInfoRequestDto;
import co.codewizards.cloudstore.ls.core.dto.RepoInfoResponseDto;
import co.codewizards.cloudstore.ls.core.invoke.ObjectRef;

public class CloudStoreJaxbContextProviderImpl extends AbstractCloudStoreJaxbContextProvider {

	@Override
	public Class<?>[] getClassesToBeBound() {
		return new Class<?>[] {
				ObjectRef.class,
				RemoteRepositoryDto.class,
				RepoInfoRequestDto.class,
				RepoInfoResponseDto.class
		};
	}

}
