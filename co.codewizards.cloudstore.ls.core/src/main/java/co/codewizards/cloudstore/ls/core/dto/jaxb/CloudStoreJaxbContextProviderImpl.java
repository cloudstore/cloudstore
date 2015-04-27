package co.codewizards.cloudstore.ls.core.dto.jaxb;

import co.codewizards.cloudstore.core.dto.jaxb.AbstractCloudStoreJaxbContextProvider;
import co.codewizards.cloudstore.ls.core.dto.TestDto;

public class CloudStoreJaxbContextProviderImpl extends AbstractCloudStoreJaxbContextProvider {

	@Override
	public Class<?>[] getClassesToBeBound() {
		return new Class<?>[] {
				TestDto.class
		};
	}

}
