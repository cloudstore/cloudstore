/**
 * Transport abstraction.
 * <p>
 * The synchronisation logic accesses all repositories through this abstraction layer. Therefore,
 * the synchronisation logic does not need to know any details about how to communicate with
 * a repository.
 * <p>
 * There are currently two implementations:
 * <ul>
 * <li>file-system-based (for local repositories)
 * <li>REST-based (for remote repositories)
 * </ul>
 * Further implementations might be written later.
 * <p>
 * The most important class in this package is the interface
 * {@link co.codewizards.cloudstore.core.repo.transport.RepoTransport RepoTransport}.
 */
package co.codewizards.cloudstore.core.repo.transport;