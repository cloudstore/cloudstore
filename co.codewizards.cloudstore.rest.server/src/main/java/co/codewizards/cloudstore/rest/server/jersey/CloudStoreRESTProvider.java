package co.codewizards.cloudstore.rest.server.jersey;

import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

import co.codewizards.cloudstore.rest.server.CloudStoreREST;

import com.sun.jersey.spi.inject.SingletonTypeInjectableProvider;

@Provider
public class CloudStoreRESTProvider
extends SingletonTypeInjectableProvider<Context, CloudStoreREST>
{
	public CloudStoreRESTProvider(CloudStoreREST instance) {
		super(CloudStoreREST.class, instance);
	}
}
