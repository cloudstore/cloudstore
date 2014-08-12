/**
 * Authentication- and authorisation-related classes - used in multiple locations.
 * <p>
 * When synchronising data between two repositories, CloudStore does not use the classic authentication based
 * on a username and a password. Instead, what we call repo-to-repo-authentication happens as follows:
 * <p>
 * Every repository has its own public-private-key-pair. When connecting two repositories, the two repositories
 * exchange their public keys - which are then known and trusted by each other.
 * <p>
 * Whenever the client needs to communicate with the HTTPS server, it first asks the server for an auth-token.
 * This auth-token is a very long random password, which is valid only for a pretty short time (around 1 hour).
 * <p>
 * All requests serving the synchronisation of two repositories are done by the client on behalf of a certain
 * repository located on the client. We thus call this the client-repository. This is one side of the
 * synchronisation. On the other side - the server-side -, there is again one certain repository: the
 * server-repository.
 * <p>
 * The auth-token is generated for this individual client-repository and this individual server-repository,
 * only. It cannot be used to communicate with another repository on the server.
 * <p>
 * Since the server-repository knows (and trusts) the client-repository, it can easily make sure, only the
 * intended client-repository can access the auth-token: It encrypts it with the client-repository's
 * public key.
 * <p>
 * Because it must also be possible for the client to verify whether the auth-token is really originating from
 * the correct server-repository, the auth-token is additionally signed by the server-repository. The
 * client-repository knows and trusts its public key and can thus verify this signature. This mechanism
 * adds security to the already encrypted HTTPS transport layer (which might be important in certain use cases,
 * e.g. when using "normal" certificates signed by a public CA).
 */
package co.codewizards.cloudstore.core.auth;