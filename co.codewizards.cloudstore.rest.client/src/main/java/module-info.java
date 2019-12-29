open module co.codewizards.cloudstore.rest.client {

	requires transitive co.codewizards.cloudstore.rest.shared;

	requires transitive jersey.client;

	exports co.codewizards.cloudstore.rest.client;
	exports co.codewizards.cloudstore.rest.client.request;
	exports co.codewizards.cloudstore.rest.client.ssl;
	exports co.codewizards.cloudstore.rest.client.transport;

}