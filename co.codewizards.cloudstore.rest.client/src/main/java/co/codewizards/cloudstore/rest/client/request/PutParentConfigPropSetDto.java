package co.codewizards.cloudstore.rest.client.request;

import static java.util.Objects.*;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import co.codewizards.cloudstore.core.dto.ConfigPropSetDto;

public class PutParentConfigPropSetDto extends VoidRequest {

	private final String repositoryName;
	private final ConfigPropSetDto parentConfigPropSetDto;

	public PutParentConfigPropSetDto(final String repositoryName, final ConfigPropSetDto parentConfigPropSetDto) {
		this.repositoryName = requireNonNull(repositoryName, "repositoryName");
		this.parentConfigPropSetDto = requireNonNull(parentConfigPropSetDto, "parentConfigPropSetDto");
	}

	@Override
	protected Response _execute() {
		return assignCredentials(
				createWebTarget("_putParentConfigPropSetDto", urlEncode(repositoryName)).request())
				.put(Entity.entity(parentConfigPropSetDto, MediaType.APPLICATION_XML_TYPE));
	}

}
