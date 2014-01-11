/*
 * Cumulus4j - Securing your data in the cloud - http://cumulus4j.org
 * Copyright (C) 2011 NightLabs Consulting GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package co.codewizards.cloudstore.client;

import java.io.File;

import org.kohsuke.args4j.Option;

import co.codewizards.cloudstore.core.dto.EntityID;
import co.codewizards.cloudstore.core.persistence.RemoteRepositoryRequest;
import co.codewizards.cloudstore.core.persistence.RemoteRepositoryRequestDAO;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerFactory;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;

/**
 * {@link SubCommand} implementation for requesting a connection at a remote repository.
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
public class AcceptRepoConnectionSubCommand extends SubCommand
{
	@Option(name="-local", metaVar="<path>", required=false, usage="A path inside a repository in the local file system. This may be the local repository's root or any directory inside it. If it is not specified, it defaults to the current working directory. If this is a sub-directory, the repository's root is automatically determined.")
	private String local;

	private File localFile;

	@Option(name="-remoteID", metaVar="<uuid>", required=false, usage="The unique ID of a remote repository currently requesting to be connected. If none is specified, the oldest request is accepted.")
	private String remoteID;

	private EntityID remoteRepositoryID;

	@Override
	public String getSubCommandName() {
		return "acceptRepoConnection";
	}

	@Override
	public String getSubCommandDescription() {
		return "Accept a connection request from a remote repository.";
	}

	@Override
	public void prepare() throws Exception {
		super.prepare();

		if (local == null)
			localFile = new File("").getAbsoluteFile();
		else
			localFile = new File(local).getAbsoluteFile();

		local = localFile.getPath();

		remoteRepositoryID = remoteID == null ? null : new EntityID(remoteID);
	}

	@Override
	public void run() throws Exception {
		File localRoot = localFile;
		// TODO automatically find the root!
		LocalRepoManager localRepoManager = LocalRepoManagerFactory.getInstance().createLocalRepoManagerForExistingRepository(localRoot);
		try {
			byte[] publicKey;
			LocalRepoTransaction transaction = localRepoManager.beginTransaction();
			try {
				RemoteRepositoryRequestDAO remoteRepositoryRequestDAO = transaction.getDAO(RemoteRepositoryRequestDAO.class);
				RemoteRepositoryRequest request;
				if (remoteRepositoryID == null) {
					RemoteRepositoryRequest oldestRequest = null;
					for (RemoteRepositoryRequest remoteRepositoryRequest : remoteRepositoryRequestDAO.getObjects()) {
						if (oldestRequest == null || oldestRequest.getChanged().after(remoteRepositoryRequest.getChanged()))
							oldestRequest = remoteRepositoryRequest;
					}
					if (oldestRequest == null)
						throw new IllegalStateException("There is no connection request pending for this local repository: " + localRoot.getPath());

					request = oldestRequest;
				}
				else {
					request = remoteRepositoryRequestDAO.getRemoteRepositoryRequestOrFail(remoteRepositoryID);
				}
				remoteRepositoryID = request.getRepositoryID();
				publicKey = request.getPublicKey();
				transaction.commit();
			} finally {
				transaction.rollbackIfActive();
			}
			localRepoManager.putRemoteRepository(remoteRepositoryID, null, publicKey); // deletes the request.
		} finally {
			localRepoManager.close();
		}
	}
}
