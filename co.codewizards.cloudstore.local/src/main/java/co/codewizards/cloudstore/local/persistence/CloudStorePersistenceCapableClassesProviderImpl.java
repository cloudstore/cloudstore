package co.codewizards.cloudstore.local.persistence;


public class CloudStorePersistenceCapableClassesProviderImpl extends
		AbstractCloudStorePersistenceCapableClassesProvider {

	@Override
	public Class<?>[] getPersistenceCapableClasses() {
		return new Class<?>[] {
				CopyModification.class,
				DeleteModification.class,
				Directory.class,
				Entity.class,
				FileChunk.class,
				LastSyncToRemoteRepo.class,
				LocalRepository.class,
				Modification.class,
				NormalFile.class,
				RemoteRepository.class,
				RemoteRepositoryRequest.class,
				Repository.class,
				RepoFile.class,
				FileInProgressMarker.class,
				Symlink.class,
				TransferDoneMarker.class
		};
	}

}
