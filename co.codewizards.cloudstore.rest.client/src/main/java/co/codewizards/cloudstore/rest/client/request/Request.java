package co.codewizards.cloudstore.rest.client.request;

import co.codewizards.cloudstore.rest.client.CloudStoreRestClient;

/**
 * REST request sending data to / querying data from / invoking logic on the server.
 * <p>
 * Every REST request should be a separate class implementing this interface. It should be instantiated for
 * an individual invocation, parameterised (usually directly via the constructor) and passed to
 * {@link CloudStoreRestClient#execute(Request)}.
 * <p>
 * Objects of this type are therefore short-lived: They normally are only used for one single invocation and
 * forgotten afterwards. In most cases, anonymous instances are directly passed to the
 * {@code CloudStoreRestClient.execute(...)} method as shown in this example:
 * <p>
 * <pre>return getCloudStoreRestClient().execute(new DoThisAndThatOnServer(param1, param2));</pre>
 * <p>
 * Implementations of this interface are <i>not</i> thread-safe.
 * <p>
 * <b>Important:</b> Please do <i>not</i> directly implement this interface! If the REST request queries a
 * response from the server, it is recommended to sub-class {@link AbstractRequest}. If there is no response,
 * implementors should sub-class {@link VoidRequest} instead.
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 *
 * @param <R> the response type, i.e. the type of the object sent from the server back to the client.
 */
public interface Request<R> {

	/**
	 * Gets the {@code CloudStoreRestClient}.
	 * <p>
	 * {@link CloudStoreRestClient#execute(Request)} assigns this property, before invoking
	 * {@link #execute()}. After the invocation, this property is cleared, again.
	 * @return the {@code CloudStoreRestClient}. Never <code>null</code> during
	 * {@linkplain #execute() execution} (but otherwise it normally is <code>null</code>).
	 * @see #setCloudStoreRESTClient(CloudStoreRestClient)
	 */
	CloudStoreRestClient getCloudStoreRESTClient();

	/**
	 * Sets the {@code CloudStoreRestClient}.
	 * @param cloudStoreRestClient the {@code CloudStoreRestClient}. May be <code>null</code>.
	 * @see #getCloudStoreRESTClient()
	 */
	void setCloudStoreRESTClient(CloudStoreRestClient cloudStoreRestClient);

	/**
	 * Execute the actual request.
	 * <p>
	 * <b>Important:</b> You should never invoke this method directly! Instead, pass the {@code Request} to
	 * {@link CloudStoreRestClient#execute(Request)}.
	 * @return the response from the server. May be <code>null</code>. Depending on
	 * {@link #isResultNullable()} a <code>null</code> result is considered an error and causes an exception.
	 */
	R execute();

	/**
	 * Indicates, if the result of the invocation can be <code>null</code>.
	 * <p>
	 * If the server <i>must</i> send a response, i.e. the invocation must not return empty-handed, this
	 * should be <code>false</code>. In case, the server still does not send a reply, it is considered an
	 * error causing an exception.
	 * <p>
	 * Please note: If a request <i>never</i> returns a response (like a Java void method), it is recommended
	 * that you sub-class {@link VoidRequest}.
	 * @return <code>true</code> if <code>null</code> as response is allowed; <code>false</code> otherwise.
	 */
	boolean isResultNullable();

}
