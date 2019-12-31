open module co.codewizards.cloudstore.rest.server {

	requires transitive co.codewizards.cloudstore.rest.shared;

	requires transitive jakarta.inject;
	requires transitive jersey.server;
	requires transitive javax.servlet.api;

	exports co.codewizards.cloudstore.rest.server;
	exports co.codewizards.cloudstore.rest.server.auth;
	exports co.codewizards.cloudstore.rest.server.ldap;
	exports co.codewizards.cloudstore.rest.server.service;
	exports co.codewizards.cloudstore.rest.server.webdav;
}