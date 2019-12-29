open module co.codewizards.cloudstore.ls.server.cproc {

	requires transitive co.codewizards.cloudstore.ls.server;
	
	requires transitive ch.qos.logback.core;
	requires transitive ch.qos.logback.classic;

	exports co.codewizards.cloudstore.ls.server.cproc;

}