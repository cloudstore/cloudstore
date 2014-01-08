package co.codewizards.cloudstore.rest.server.jersey;

import javax.inject.Singleton;

import org.glassfish.hk2.utilities.binding.AbstractBinder;

import co.codewizards.cloudstore.rest.server.CloudStoreREST;

public class CloudStoreBinder extends AbstractBinder {

	@Override
	protected void configure() {
		bind(CloudStoreREST.class).to(CloudStoreREST.class).in(Singleton.class);
	}

}
