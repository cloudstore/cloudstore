open module co.codewizards.cloudstore.server {

	requires transitive org.eclipse.jetty.server;
	requires transitive org.eclipse.jetty.servlet;
	requires transitive org.eclipse.jetty.util;

	requires transitive jersey.container.servlet;
	requires transitive jersey.container.servlet.core;

	requires transitive ch.qos.logback.core;
	requires transitive ch.qos.logback.classic;

	requires transitive co.codewizards.cloudstore.rest.server;
	requires transitive co.codewizards.cloudstore.ls.server;

	exports co.codewizards.cloudstore.server;

}