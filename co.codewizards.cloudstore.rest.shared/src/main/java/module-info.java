open module co.codewizards.cloudstore.rest.shared {

	requires transitive java.ws.rs;

	requires transitive co.codewizards.cloudstore.core;

	requires transitive jersey.common;

	exports co.codewizards.cloudstore.rest.shared;
	exports co.codewizards.cloudstore.rest.shared.filter;
	exports co.codewizards.cloudstore.rest.shared.interceptor;

}