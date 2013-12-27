package co.codewizards.cloudstore.test;


import org.junit.Test;

import co.codewizards.cloudstore.client.test.ServiceInvoke;

public class TestServiceTest {

	@Test
	public void testInvokingREST() {
		ServiceInvoke serviceInvoke = new ServiceInvoke("http", "co.codewizards.cloudstore", 4000);
		serviceInvoke.testInvokingREST();
	}
}
