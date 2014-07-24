package co.codewizards.cloudstore.rest.client.command;

import co.codewizards.cloudstore.rest.client.CloudStoreRestClient;

public interface Command<R> {

	CloudStoreRestClient getCloudStoreRESTClient();
	void setCloudStoreRESTClient(CloudStoreRestClient cloudStoreRestClient);

	R execute();

	boolean isResultNullable();

}
