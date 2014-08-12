/**
 * Server (separate program).
 * <p>
 * The CloudStore server is needed to synchronise data between a local and a remote repository. At least one
 * of the machines must run the server process and thus listen to TCP connections. More precisely REST over
 * HTTPS is used for client-server-communication.
 * <p>
 * The server is run via <code>$INSTALLATION_DIR/bin/cloudstore-server</code> (or
 * <code>$INSTALLATION_DIR/bin/cloudstore-server.bat</code> on Windows). Since this script blocks while the
 * server is running, it is recommended to use one of the start-scripts instead:
 * <ul>
 * <li><code>$INSTALLATION_DIR/etc/init/cloudstore-server.conf</code> for <a href="http://en.wikipedia.org/wiki/Upstart">Upstart</a>.
 * <li><code>$INSTALLATION_DIR/etc/init.d/cloudstore-server</code> for <a href="http://en.wikipedia.org/wiki/Sysvinit">SysV-init</a>.
 * </ul>
 * <p>
 * You must copy <b>one</b> of the start scripts into the appropriate directory. Note, that you might need to
 * change the start script to fit your needs.
 */
package co.codewizards.cloudstore.server;