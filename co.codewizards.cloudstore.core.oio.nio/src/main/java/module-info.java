open module co.codewizards.cloudstore.core.oio.nio {

	requires transitive co.codewizards.cloudstore.core;

	exports co.codewizards.cloudstore.core.oio.nio;

	provides co.codewizards.cloudstore.core.oio.FileFactory
		with co.codewizards.cloudstore.core.oio.nio.NioFileFactory;
}