package co.codewizards.cloudstore.rest.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.MalformedURLException;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;

import mockit.Mocked;
import mockit.StrictExpectations;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;
import net.jcip.annotations.NotThreadSafe;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JMockit.class)
@NotThreadSafe
public class CloudStoreRestClientTest{

	private CloudStoreRestClient cloudstoreClient;
	@Mocked
	private ClientBuilder clientBuilder;
	@Mocked
	private Client client;


	@Test(expected = IllegalStateException.class)
	public void baseUrlNotFound() throws MalformedURLException {
		new StrictExpectations() {{
			clientBuilder.build();
			result = client;
			client.register(any); result = client;

			client.target("https://localhost:8080/_test").request(MediaType.TEXT_PLAIN).get(String.class);
			result = new WebApplicationException();
		}};
		cloudstoreClient = new CloudStoreRestClient("https://localhost:8080/", clientBuilder);

		cloudstoreClient.getBaseUrl();
	}

	@Test
	public void successAtTheFirstCall() {
		new StrictExpectations() {{
			clientBuilder.build(); result = client;
			client.register(any); result = client;

			client.target("https://localhost:8080/_test").request(MediaType.TEXT_PLAIN).get(String.class);
			result = "SUCCESS";
		}};
		cloudstoreClient = new CloudStoreRestClient("https://localhost:8080/aaa/bbb", clientBuilder);

		String result = cloudstoreClient.getBaseUrl();
		assertThat(result).isEqualTo("https://localhost:8080/");

		new Verifications() {{
		    client.target("https://localhost:8080/aaa/_test"); times = 0;
		    client.target("https://localhost:8080/aaa/bbb/_test"); times = 0;
		}};

	}

	@Test
	public void urlIsBaseUrl() {
		new StrictExpectations() {{
			clientBuilder.build(); result = client;
			client.register(any); result = client;

			client.target("https://localhost:8080/_test").request(MediaType.TEXT_PLAIN).get(String.class);
			result = "SUCCESS";
		}};
		cloudstoreClient = new CloudStoreRestClient("https://localhost:8080/", clientBuilder);


		String result = cloudstoreClient.getBaseUrl();
		assertThat(result).isEqualTo("https://localhost:8080/");
	}

	@Test
	public void doubleSlashInUrl() {
		new StrictExpectations() {{
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
		cloudstoreClient = new CloudStoreRestClient("https://localhost:8080//aaa//bbb/", clientBuilder);

		String result = cloudstoreClient.getBaseUrl();
		assertThat(result).isEqualTo("https://localhost:8080/aaa/bbb/");
	}

	@Test
	public void successAtTheLastCall() {
		new StrictExpectations() {{
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
		cloudstoreClient = new CloudStoreRestClient("https://localhost:8080/aaa/bbb/ccc", clientBuilder);

		String result = cloudstoreClient.getBaseUrl();
		assertThat(result).isEqualTo("https://localhost:8080/aaa/bbb/ccc/");
	}

	@Test
	public void successAtTheMiddleCall() {
		new StrictExpectations() {{
			clientBuilder.build(); result = client;
			client.register(any); result = client;

			client.target("https://localhost:8080/_test").request(MediaType.TEXT_PLAIN).get(String.class);
			result = new WebApplicationException();
			client.target("https://localhost:8080/aaa/_test").request(MediaType.TEXT_PLAIN).get(String.class);
			result = new WebApplicationException();
			client.target("https://localhost:8080/aaa/bbb/_test").request(MediaType.TEXT_PLAIN).get(String.class);
			result = "SUCCESS";
		}};
		cloudstoreClient = new CloudStoreRestClient("https://localhost:8080/aaa/bbb/ccc", clientBuilder);


		String result = cloudstoreClient.getBaseUrl();
		assertThat(result).isEqualTo("https://localhost:8080/aaa/bbb/");

		new Verifications() {{
		    client.target("https://localhost:8080/aaa/bbb/ccc/_test"); times = 0;
		}};
	}

	@Test
	public void urlWithoutPort() {
		new StrictExpectations() {{
			clientBuilder.build(); result = client;
			client.register(any); result = client;

			client.target("https://cloudstore.codewizards.co/_test").request(MediaType.TEXT_PLAIN).get(String.class);
			result = new WebApplicationException();
			client.target("https://cloudstore.codewizards.co/mediathek/_test").request(MediaType.TEXT_PLAIN).get(String.class);
			result = "SUCCESS";
		}};
		cloudstoreClient = new CloudStoreRestClient("https://cloudstore.codewizards.co/mediathek/musik", clientBuilder);

		String result = cloudstoreClient.getBaseUrl();
		assertThat(result).isEqualTo("https://cloudstore.codewizards.co/mediathek/");

		new Verifications() {{
		    client.target("https://cloudstore.codewizards.co/mediathek/musik/_test"); times = 0;
		 }};
	}

	@Test
	public void urlWithoutSlashAtTheEnd() {
		new StrictExpectations() {{
			clientBuilder.build(); result = client;
			client.register(any); result = client;

			client.target("https://cloudstore.codewizards.co/_test").request(MediaType.TEXT_PLAIN).get(String.class);
			result = "SUCCESS";
		}};
		cloudstoreClient = new CloudStoreRestClient("https://cloudstore.codewizards.co", clientBuilder);

		String result = cloudstoreClient.getBaseUrl();
		assertThat(result).isEqualTo("https://cloudstore.codewizards.co/");
	}
}
