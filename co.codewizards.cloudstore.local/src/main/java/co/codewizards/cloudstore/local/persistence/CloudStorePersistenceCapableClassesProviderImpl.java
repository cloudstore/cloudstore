package co.codewizards.cloudstore.local.persistence;


public class CloudStorePersistenceCapableClassesProviderImpl extends
		AbstractCloudStorePersistenceCapableClassesProvider {

	@Override
	public int getOrderHint() {
		return 0;
	}

	@Override
	public Class<?>[] getPersistenceCapableClasses() {
		return new Class<?>[] {
				CopyModification.class,
				DeleteModification.class,
				Directory.class,
				FileChunk.class,
				LastSyncToRemoteRepo.class,
				LocalRepository.class,
				Modification.class,
				NormalFile.class,
				RemoteRepository.class,
				RemoteRepositoryRequest.class,
				Repository.class,
				FileInProgressMarker.class,
				Symlink.class
		};
	}

}
