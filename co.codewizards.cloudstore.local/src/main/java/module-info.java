open module co.codewizards.cloudstore.local {

	requires transitive javax.jdo;
	requires transitive org.datanucleus;

	requires transitive co.codewizards.cloudstore.core;

	requires transitive log4j;

	exports co.codewizards.cloudstore.local;
	exports co.codewizards.cloudstore.local.db;
	exports co.codewizards.cloudstore.local.dto;
	exports co.codewizards.cloudstore.local.persistence;
	exports co.codewizards.cloudstore.local.transport;

}