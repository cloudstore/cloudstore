open module co.codewizards.cloudstore.core {

//	requires transitive java.base; // https://bugs.openjdk.java.net/browse/JDK-8182734

requires transitive java.se;

	requires transitive java.xml;
	requires transitive java.xml.bind;
	requires transitive com.sun.xml.bind;

	requires transitive org.slf4j;

	requires transitive org.bouncycastle.provider;

	exports co.codewizards.cloudstore.core;
	exports co.codewizards.cloudstore.core.appid;
	exports co.codewizards.cloudstore.core.auth;
	exports co.codewizards.cloudstore.core.bean;
	exports co.codewizards.cloudstore.core.chronos;
	exports co.codewizards.cloudstore.core.collection;
	exports co.codewizards.cloudstore.core.concurrent;
	exports co.codewizards.cloudstore.core.config;
	exports co.codewizards.cloudstore.core.context;
	exports co.codewizards.cloudstore.core.dto;
	exports co.codewizards.cloudstore.core.dto.jaxb;
	exports co.codewizards.cloudstore.core.exception;
	exports co.codewizards.cloudstore.core.ignore;
	exports co.codewizards.cloudstore.core.io;
	exports co.codewizards.cloudstore.core.ls;
	exports co.codewizards.cloudstore.core.objectfactory;
	exports co.codewizards.cloudstore.core.oio;
	exports co.codewizards.cloudstore.core.otp;
	exports co.codewizards.cloudstore.core.progress;
	exports co.codewizards.cloudstore.core.ref;
	exports co.codewizards.cloudstore.core.repo.local;
	exports co.codewizards.cloudstore.core.repo.sync;
	exports co.codewizards.cloudstore.core.repo.transport;
	exports co.codewizards.cloudstore.core.sync;
	exports co.codewizards.cloudstore.core.updater;
	exports co.codewizards.cloudstore.core.util;
	exports co.codewizards.cloudstore.core.util.childprocess;
	exports co.codewizards.cloudstore.core.version;

	uses co.codewizards.cloudstore.core.appid.AppId;
	uses co.codewizards.cloudstore.core.chronos.Chronos;
	uses co.codewizards.cloudstore.core.dto.jaxb.CloudStoreJaxbContextProvider;
	uses co.codewizards.cloudstore.core.objectfactory.ClassExtension;
	uses co.codewizards.cloudstore.core.oio.FileFactory;
	uses co.codewizards.cloudstore.core.repo.local.LocalRepoManagerFactory;
	uses co.codewizards.cloudstore.core.repo.local.LocalRepoTransactionListener;
	uses co.codewizards.cloudstore.core.repo.transport.RepoTransportFactory;

	provides co.codewizards.cloudstore.core.appid.AppId
		with co.codewizards.cloudstore.core.appid.CloudStoreAppId;

	provides co.codewizards.cloudstore.core.chronos.Chronos
	with co.codewizards.cloudstore.core.chronos.DefaultChronosImpl;

	provides co.codewizards.cloudstore.core.dto.jaxb.CloudStoreJaxbContextProvider
		with co.codewizards.cloudstore.core.dto.jaxb.CloudStoreJaxbContextProviderImpl;

	provides co.codewizards.cloudstore.core.oio.FileFactory
		with co.codewizards.cloudstore.core.oio.IoFileFactory;
}