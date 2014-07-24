package co.codewizards.cloudstore.rest.client.command;

import static co.codewizards.cloudstore.core.util.Util.*;

import javax.ws.rs.core.Response;

public class Move extends VoidCommand {

	private final String repositoryName;
	private final String fromPath;
	private final String toPath;

	public Move(final String repositoryName, final String fromPath, final String toPath) {
		this.repositoryName = assertNotNull("repositoryName", repositoryName);
		this.fromPath = assertNotNull("fromPath", fromPath);
		this.toPath = assertNotNull("toPath", toPath);
	}

	@Override
	protected Response _execute() {
		return assignCredentials(createWebTarget("_move", urlEncode(repositoryName), encodePath(fromPath))
				.queryParam("to", encodePath(toPath))
				.request()).post(null);
	}

}
