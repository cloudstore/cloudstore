/**
 * Local repository management - implementation.
 * <p>
 * The primary reason for separating this package from <code>co.codewizards.cloudstore.core.repo.local</code>
 * and locating it in a separate library is that <code>co.codewizards.cloudstore.local</code> requires
 * additional dependencies - most importantly <a href="http://www.datanucleus.org">DataNucleus</a>.
 * <p>
 * Additionally, the separation of the implementation from the API helps to keep the code loosely coupled.
 * <p>
 * The most important classes here are:
 * <ul>
 * <li>{@link co.codewizards.cloudstore.local.LocalRepoManagerFactoryImpl LocalRepoManagerFactoryImpl}
 * <li>{@link co.codewizards.cloudstore.local.LocalRepoManagerImpl LocalRepoManagerImpl}
 * <li>{@link co.codewizards.cloudstore.local.LocalRepoSync LocalRepoSync}
 * </ul>
 */
package co.codewizards.cloudstore.local;