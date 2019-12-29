open module co.codewizards.cloudstore.rest.shared {

	requires transitive java.ws.rs;

	requires transitive co.codewizards.cloudstore.core;

	exports co.codewizards.cloudstore.rest.shared;
	exports co.codewizards.cloudstore.rest.shared.filter;
	exports co.codewizards.cloudstore.rest.shared.interceptor;

	requires transitive jersey.common;
}