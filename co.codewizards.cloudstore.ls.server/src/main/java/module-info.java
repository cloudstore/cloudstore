open module co.codewizards.cloudstore.ls.server {

	requires transitive co.codewizards.cloudstore.ls.rest.server;

	requires transitive org.eclipse.jetty.server;
	requires transitive org.eclipse.jetty.servlet;
	requires transitive org.eclipse.jetty.util;

	requires transitive jersey.container.servlet;
	requires transitive jersey.container.servlet.core;

	exports co.codewizards.cloudstore.ls.server;

}