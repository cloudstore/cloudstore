/**
 * Command-line client.
 * <p>
 * The CloudStore command-line client is able to synchronise data between two local repositories using direct
 * file-access as well as to synchronise a local repository with a remote repository accessible via REST.
 * <p>
 * Furthermore, the command-line client can be used for administrative tasks like creating a repository,
 * requesting or accepting a repository connection and many more.
 * <p>
 * The command-line client is invoked via <code>$INSTALLATION_DIR/bin/cloudstore</code> (or
 * <code>$INSTALLATION_DIR/bin/cloudstore.bat</code> on Windows). If you add this
 * <code>bin/</code> directory to your <code>PATH</code>, which is recommended, you can invoke cloudstore by
 * simply executing the command <code>cloudstore</code>.
 * <p>
 * Said command is a simple shell/batch script invoking
 * <code>java -jar "$INSTALLATION_DIR/lib/co.codewizards.cloudstore.client-VERSION.jar"</code> with all
 * parameters passed through.
 * <p>
 * To get a list of available sub-commands, please execute the command <code>cloudstore help</code>. You can
 * get detailed information about each sub-command by invoking <code>cloudstore help &lt;subCommand&gt;</code>.
 * <p>
 * All sub-commands are sub-classes of {@link co.codewizards.cloudstore.client.SubCommand SubCommand}.
 */
package co.codewizards.cloudstore.client;