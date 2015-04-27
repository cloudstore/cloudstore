package co.codewizards.cloudstore.ls.rest.server;

import javax.inject.Singleton;

import org.glassfish.hk2.utilities.binding.AbstractBinder;

public class LocalServerRestBinder extends AbstractBinder {

	@Override
	protected void configure() {
		bind(LocalServerRest.class).to(LocalServerRest.class).in(Singleton.class);
	}

}
