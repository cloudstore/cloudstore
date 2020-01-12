open module co.codewizards.cloudstore.updater {
	
	requires transitive args4j;

	requires transitive ch.qos.logback.core;
	requires transitive ch.qos.logback.classic;

	requires transitive org.apache.commons.compress;

	requires transitive org.bouncycastle.pg;

	requires transitive co.codewizards.cloudstore.core;

	exports co.codewizards.cloudstore.updater;

}