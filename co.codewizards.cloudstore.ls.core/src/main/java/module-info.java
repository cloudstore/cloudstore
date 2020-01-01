open module co.codewizards.cloudstore.ls.core {

	requires transitive java.ws.rs;

	requires transitive co.codewizards.cloudstore.core;

	requires transitive jersey.client;
	requires transitive jersey.common;

	uses co.codewizards.cloudstore.ls.core.invoke.filter.InvocationFilter;
	uses co.codewizards.cloudstore.ls.core.invoke.refjanitor.ReferenceJanitor;

	exports co.codewizards.cloudstore.ls.core;
	exports co.codewizards.cloudstore.ls.core.dto;
	exports co.codewizards.cloudstore.ls.core.dto.jaxb;
	exports co.codewizards.cloudstore.ls.core.invoke;
	exports co.codewizards.cloudstore.ls.core.invoke.filter;
	exports co.codewizards.cloudstore.ls.core.invoke.refjanitor;
	exports co.codewizards.cloudstore.ls.core.provider;
}