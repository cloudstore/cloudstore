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

import org.kohsuke.args4j.Argument;

import co.codewizards.cloudstore.core.dto.EntityID;
import co.codewizards.cloudstore.core.persistence.RemoteRepositoryRequest;
import co.codewizards.cloudstore.core.persistence.RemoteRepositoryRequestDAO;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerFactory;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.core.util.HashUtil;

/**
 * {@link SubCommand} implementation for requesting a connection at a remote repository.
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
public class AcceptRepoConnectionSubCommand extends SubCommandWithExistingLocalRepo
{
	@Argument(metaVar="<remote>", index=1, required=false, usage="The unique ID of a remote repository currently requesting to be connected. If none is specified, the oldest request is accepted.")
	private String remote;

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
		remoteRepositoryID = remote == null ? null : new EntityID(remote);
	}

	@Override
	public void run() throws Exception {
		EntityID localRepositoryID;
		byte[] localPublicKey;
		byte[] remotePublicKey;
		LocalRepoManager localRepoManager = LocalRepoManagerFactory.getInstance().createLocalRepoManagerForExistingRepository(localRoot);
		try {
			localRepositoryID = localRepoManager.getRepositoryID();
			localPublicKey = localRepoManager.getPublicKey();
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
				remotePublicKey = request.getPublicKey();
				transaction.commit();
			} finally {
				transaction.rollbackIfActive();
			}
			localRepoManager.putRemoteRepository(remoteRepositoryID, null, remotePublicKey); // deletes the request.
		} finally {
			localRepoManager.close();
		}

		System.out.println("Successfully accepted the connection request for the following local and remote repositories:");
		System.out.println();
		System.out.println("  localRepository.repositoryID = " + localRepositoryID);
		System.out.println("  localRepository.localRoot = " + localRoot);
		System.out.println("  localRepository.publicKeySha1 = " + HashUtil.sha1ForHuman(localPublicKey));
		System.out.println();
		System.out.println("  remoteRepository.repositoryID = " + remoteRepositoryID);
		System.out.println("  remoteRepository.publicKeySha1 = " + HashUtil.sha1ForHuman(remotePublicKey));
		System.out.println();
		System.out.println("Please verify the 'publicKeySha1' fingerprints! If they do not match the fingerprints shown on the client, someone is attacking you and you must cancel this request immediately! To cancel the request, use this command:");
		System.out.println();
		System.out.println(String.format("  cloudstore cancelRepoConnection %s %s", localRepositoryID, remoteRepositoryID));
	}
}
