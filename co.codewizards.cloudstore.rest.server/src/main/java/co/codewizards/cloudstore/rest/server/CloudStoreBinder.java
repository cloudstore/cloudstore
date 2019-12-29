package co.codewizards.cloudstore.rest.server;

import javax.inject.Singleton;

import org.glassfish.jersey.internal.inject.AbstractBinder;

//import org.glassfish.hk2.utilities.binding.AbstractBinder;

public class CloudStoreBinder extends AbstractBinder {

	@Override
	protected void configure() {
		bind(CloudStoreRest.class).to(CloudStoreRest.class).in(Singleton.class);
	}

}
