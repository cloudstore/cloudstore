package co.codewizards.cloudstore.rest.server.service;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.dto.VersionInfoDto;
import co.codewizards.cloudstore.core.version.VersionInfoProvider;

@Path("_VersionInfoDto")
@Consumes(MediaType.APPLICATION_XML)
@Produces(MediaType.APPLICATION_XML)
public class VersionInfoDtoService extends AbstractServiceWithRepoToRepoAuth
{
	private static final Logger logger = LoggerFactory.getLogger(VersionInfoDtoService.class);

	{
		logger.debug("<init>: created new instance");
	}

	@GET
	public VersionInfoDto getVersionInfoDto()
	{
		return VersionInfoProvider.getInstance().getVersionInfoDto();
	}
}
