package co.codewizards.cloudstore.core.repo.local;

import java.util.EventListener;

/**
 * Listener notified when a {@link LocalRepoManager} is closed.
 * <p>
 * <b>Important:</b> If registered on a
 * {@link LocalRepoManager#addLocalRepoManagerCloseListener(LocalRepoManagerCloseListener) LocalRepoManager},
 * this listener is notified when the proxy is closed <i>and</i> when the real backend-instance is closed.
 * The {@linkplain LocalRepoManagerCloseEvent#isBackend() event's <code>backend</code> property}
 * indicates whether the real backend was closed. If registered on the
 * {@link LocalRepoManagerFactory#addLocalRepoManagerCloseListener(LocalRepoManagerCloseListener) LocalRepoManagerFactory},
 * it is notified only about real backend instances being closed.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public interface LocalRepoManagerCloseListener extends EventListener {
	void preClose(LocalRepoManagerCloseEvent event);
	void postClose(LocalRepoManagerCloseEvent event);
}
