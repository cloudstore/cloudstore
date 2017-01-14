package co.codewizards.cloudstore.client;

import co.codewizards.cloudstore.core.dto.DateTime;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.util.AssertUtil;
import co.codewizards.cloudstore.core.util.HashUtil;
import co.codewizards.cloudstore.ls.core.dto.RemoteRepositoryDto;
import co.codewizards.cloudstore.ls.core.dto.RemoteRepositoryRequestDto;
import co.codewizards.cloudstore.ls.core.dto.RepoInfoRequestDto;
import co.codewizards.cloudstore.ls.core.dto.RepoInfoResponseDto;
import co.codewizards.cloudstore.ls.rest.client.request.RepoInfoRequest;

/**
 * {@link SubCommand} implementation for showing information about a repository in the local file system.
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
public class RepoInfoSubCommand extends SubCommandWithExistingLocalRepo
{
	public RepoInfoSubCommand() { }

	protected RepoInfoSubCommand(final File localRoot) {
		this.localRoot = AssertUtil.assertNotNull(localRoot, "localRoot");
		this.localFile = this.localRoot;
		this.local = localRoot.getPath();
	}

	@Override
	public String getSubCommandDescription() {
		return "Show information about an existing repository.";
	}

	@Override
	public void run() throws Exception {
		final RepoInfoRequestDto repoInfoRequestDto = new RepoInfoRequestDto();
		repoInfoRequestDto.setLocalRoot(localRoot.getAbsolutePath());
		final RepoInfoResponseDto repoInfoResponseDto = getLocalServerRestClient().execute(new RepoInfoRequest(repoInfoRequestDto));

		showMainProperties(repoInfoResponseDto);
		showRemoteRepositories(repoInfoResponseDto);
		showRemoteRepositoryRequests(repoInfoResponseDto);
		showRepositoryStats(repoInfoResponseDto);
	}

	private void showMainProperties(final RepoInfoResponseDto repoInfoResponseDto) {
		System.out.println("Local repository:");
		System.out.println("  repository.repositoryId = " + repoInfoResponseDto.getRepositoryId());
		System.out.println("  repository.localRoot = " + repoInfoResponseDto.getLocalRoot());
		System.out.println("  repository.aliases = " + repoInfoResponseDto);
		System.out.println("  repository.publicKeySha1 = " + HashUtil.sha1ForHuman(repoInfoResponseDto.getPublicKey()));
		System.out.println();
	}

	private void showRemoteRepositories(final RepoInfoResponseDto repoInfoResponseDto) {
		if (repoInfoResponseDto.getRemoteRepositoryDtos().isEmpty()) {
			System.out.println("Remote repositories connected: {NONE}");
			System.out.println();
		}
		else {
			System.out.println("Remote repositories connected:");
			for (final RemoteRepositoryDto remoteRepositoryDto : repoInfoResponseDto.getRemoteRepositoryDtos()) {
				System.out.println("  * remoteRepository.repositoryId = " + remoteRepositoryDto.getRepositoryId());
				if (remoteRepositoryDto.getRemoteRoot() != null)
					System.out.println("    remoteRepository.remoteRoot = " + remoteRepositoryDto.getRemoteRoot());

				System.out.println("    remoteRepository.publicKeySha1 = " + HashUtil.sha1ForHuman(remoteRepositoryDto.getPublicKey()));
				System.out.println();
			}
		}
	}

	private void showRemoteRepositoryRequests(final RepoInfoResponseDto repoInfoResponseDto) {
		if (repoInfoResponseDto.getRemoteRepositoryRequestDtos().isEmpty()) {
			System.out.println("Remote repositories requesting connection: {NONE}");
			System.out.println();
		}
		else {
			System.out.println("Remote repositories requesting connection:");
			for (final RemoteRepositoryRequestDto remoteRepositoryRequestDto : repoInfoResponseDto.getRemoteRepositoryRequestDtos()) {
				System.out.println("  * remoteRepositoryRequest.repositoryId = " + remoteRepositoryRequestDto.getRepositoryId());
				System.out.println("    remoteRepositoryRequest.publicKeySha1 = " + HashUtil.sha1ForHuman(remoteRepositoryRequestDto.getPublicKey()));
				System.out.println("    remoteRepositoryRequest.created = " + new DateTime(remoteRepositoryRequestDto.getCreated()));
				System.out.println("    remoteRepositoryRequest.changed = " + new DateTime(remoteRepositoryRequestDto.getChanged()));
				System.out.println();
			}
		}
	}

	private void showRepositoryStats(final RepoInfoResponseDto repoInfoResponseDto) {
		System.out.println("Statistics:");
		System.out.println("  * Count(NormalFile): " + repoInfoResponseDto.getNormalFileCount());
		System.out.println("  * Count(Directory): " + repoInfoResponseDto.getDirectoryCount());
		System.out.println("  * Count(CopyModification): " + repoInfoResponseDto.getCopyModificationCount());
		System.out.println("  * Count(DeleteModification): " + repoInfoResponseDto.getDeleteModificationCount());
		System.out.println();
	}
}
