open module co.codewizards.cloudstore.client {

	requires transitive args4j;

	requires transitive co.codewizards.cloudstore.local;
	requires transitive co.codewizards.cloudstore.rest.client;
	requires transitive co.codewizards.cloudstore.ls.server;
	requires transitive co.codewizards.cloudstore.ls.rest.client;
	requires transitive co.codewizards.cloudstore.ls.server.cproc;

	exports co.codewizards.cloudstore.client;

}