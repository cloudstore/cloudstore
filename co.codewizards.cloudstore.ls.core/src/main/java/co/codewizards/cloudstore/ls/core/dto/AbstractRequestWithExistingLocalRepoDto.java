package co.codewizards.cloudstore.ls.core.dto;


public abstract class AbstractRequestWithExistingLocalRepoDto {

	private String localRoot;

	/**
	 * Gets the absolute path to the local root.
	 * @return the absolute path to the local root.
	 */
	public String getLocalRoot() {
		return localRoot;
	}

	public void setLocalRoot(String localRoot) {
		this.localRoot = localRoot;
	}
}
