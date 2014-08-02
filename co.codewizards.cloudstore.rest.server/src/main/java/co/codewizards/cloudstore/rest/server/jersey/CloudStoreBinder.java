package co.codewizards.cloudstore.rest.server.jersey;

import javax.inject.Singleton;

import org.glassfish.hk2.utilities.binding.AbstractBinder;

import co.codewizards.cloudstore.rest.server.CloudStoreRest;

public class CloudStoreBinder extends AbstractBinder {

	@Override
	protected void configure() {
		bind(CloudStoreRest.class).to(CloudStoreRest.class).in(Singleton.class);
	}

}
