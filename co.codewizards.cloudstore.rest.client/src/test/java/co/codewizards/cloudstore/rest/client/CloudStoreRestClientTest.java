package co.codewizards.cloudstore.rest.client;

import static org.assertj.core.api.Assertions.assertThat;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;

import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JMockit.class)
public class CloudStoreRestClientTest {

	private CloudStoreRestClient cloudstoreClient;
	@Mocked
	private ClientBuilder clientBuilder;
	@Mocked
	private Client client;

	@Test(expected = IllegalStateException.class)
	public void baseUrlNotFound() {
		new Expectations() {{
			cloudstoreClient = new CloudStoreRestClient("https://localhost:8080/", clientBuilder);
			clientBuilder.build(); result = client;
			client.register(any); result = client;

			client.target("https://localhost:8080/_test").request(MediaType.TEXT_PLAIN).get(String.class);
			result = new WebApplicationException();
		}};

		cloudstoreClient.getBaseUrl();
	}

	@Test
	public void successAtTheFirstCall() {
		new Expectations() {{
			cloudstoreClient = new CloudStoreRestClient("https://localhost:8080/aaa/bbb", clientBuilder);
			clientBuilder.build(); result = client;
			client.register(any); result = client;

			client.target("https://localhost:8080/_test").request(MediaType.TEXT_PLAIN).get(String.class);
			result = "SUCCESS";
		}};

		String result = cloudstoreClient.getBaseUrl();
		assertThat(result).isEqualTo("https://localhost:8080/");

		new Verifications() {{
		    client.target("https://localhost:8080/aaa/_test"); times = 0;
		    client.target("https://localhost:8080/aaa/bbb/_test"); times = 0;
		}};
	}

	@Test
	public void urlIsBaseUrl() {
		new Expectations() {{
			cloudstoreClient = new CloudStoreRestClient("https://localhost:8080/", clientBuilder);
			clientBuilder.build(); result = client;
			client.register(any); result = client;

			client.target("https://localhost:8080/_test").request(MediaType.TEXT_PLAIN).get(String.class);
			result = "SUCCESS";
		}};

		String result = cloudstoreClient.getBaseUrl();
		assertThat(result).isEqualTo("https://localhost:8080/");
	}

	@Test
	public void doubleSlashInUrl() {
		new Expectations() {{
			cloudstoreClient = new CloudStoreRestClient("https://localhost:8080//aaa//bbb/", clientBuilder);
			clientBuilder.build(); result = client;
			client.register(any); result = client;

			client.target("https://localhost:8080/_test").request(MediaType.TEXT_PLAIN).get(String.class);
			result = new WebApplicationException();
			client.target("https://localhost:8080/_test").request(MediaType.TEXT_PLAIN).get(String.class);
			result = new WebApplicationException();
			client.target("https://localhost:8080/aaa/_test").request(MediaType.TEXT_PLAIN).get(String.class);
			result = new WebApplicationException();
			client.target("https://localhost:8080/aaa/_test").request(MediaType.TEXT_PLAIN).get(String.class);
			result = new WebApplicationException();
			client.target("https://localhost:8080/aaa/bbb/_test").request(MediaType.TEXT_PLAIN).get(String.class);
			result = "SUCCESS";
		}};

		String result = cloudstoreClient.getBaseUrl();
		assertThat(result).isEqualTo("https://localhost:8080/aaa/bbb/");
	}

	@Test
	public void successAtTheLastCall() {
		new Expectations() {{
			cloudstoreClient = new CloudStoreRestClient("https://localhost:8080/aaa/bbb/ccc", clientBuilder);
			clientBuilder.build(); result = client;
			client.register(any); result = client;

			client.target("https://localhost:8080/_test").request(MediaType.TEXT_PLAIN).get(String.class);
			result = new WebApplicationException();
			client.target("https://localhost:8080/aaa/_test").request(MediaType.TEXT_PLAIN).get(String.class);
			result = new WebApplicationException();
			client.target("https://localhost:8080/aaa/bbb/_test").request(MediaType.TEXT_PLAIN).get(String.class);
			result = new WebApplicationException();
			client.target("https://localhost:8080/aaa/bbb/ccc/_test").request(MediaType.TEXT_PLAIN).get(String.class);
			result = "SUCCESS";
		}};

		String result = cloudstoreClient.getBaseUrl();
		assertThat(result).isEqualTo("https://localhost:8080/aaa/bbb/ccc/");
	}

	@Test
	public void successAtTheMiddleCall() {
		new Expectations() {{
			cloudstoreClient = new CloudStoreRestClient("https://localhost:8080/aaa/bbb/ccc", clientBuilder);
			clientBuilder.build(); result = client;
			client.register(any); result = client;

			client.target("https://localhost:8080/_test").request(MediaType.TEXT_PLAIN).get(String.class);
			result = new WebApplicationException();
			client.target("https://localhost:8080/aaa/_test").request(MediaType.TEXT_PLAIN).get(String.class);
			result = new WebApplicationException();
			client.target("https://localhost:8080/aaa/bbb/_test").request(MediaType.TEXT_PLAIN).get(String.class);
			result = "SUCCESS";
		}};

		String result = cloudstoreClient.getBaseUrl();
		assertThat(result).isEqualTo("https://localhost:8080/aaa/bbb/");

		new Verifications() {{
		    client.target("https://localhost:8080/aaa/bbb/ccc/_test"); times = 0;
		 }};
	}

	@Test
	public void urlWithoutPort() {
		new Expectations() {{
			cloudstoreClient = new CloudStoreRestClient("https://cloudstore.codewizards.co/mediathek/musik", clientBuilder);
			clientBuilder.build(); result = client;
			client.register(any); result = client;

			client.target("https://cloudstore.codewizards.co/_test").request(MediaType.TEXT_PLAIN).get(String.class);
			result = new WebApplicationException();
			client.target("https://cloudstore.codewizards.co/mediathek/_test").request(MediaType.TEXT_PLAIN).get(String.class);
			result = "SUCCESS";
		}};

		String result = cloudstoreClient.getBaseUrl();
		assertThat(result).isEqualTo("https://cloudstore.codewizards.co/mediathek/");

		new Verifications() {{
		    client.target("https://cloudstore.codewizards.co/mediathek/musik/_test"); times = 0;
		 }};
	}

	@Test
	public void urlWithoutSlashAtTheEnd() {
		new Expectations() {{
			cloudstoreClient = new CloudStoreRestClient("https://cloudstore.codewizards.co", clientBuilder);
			clientBuilder.build(); result = client;
			client.register(any); result = client;

			client.target("https://cloudstore.codewizards.co/_test").request(MediaType.TEXT_PLAIN).get(String.class);
			result = "SUCCESS";
		}};

		String result = cloudstoreClient.getBaseUrl();
		assertThat(result).isEqualTo("https://cloudstore.codewizards.co/");
	}
}
