open module co.codewizards.cloudstore.ls.rest.server {

	requires transitive jakarta.inject;
	requires transitive jersey.server;
	requires transitive servlet.api;

	requires transitive co.codewizards.cloudstore.ls.core;
	requires transitive co.codewizards.cloudstore.local;

	exports co.codewizards.cloudstore.ls.rest.server;
	exports co.codewizards.cloudstore.ls.rest.server.auth;
	exports co.codewizards.cloudstore.ls.rest.server.service;
}