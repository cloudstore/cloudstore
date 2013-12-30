package co.codewizards.cloudstore.shared.repo.local;

import java.util.EventListener;

/**
 * Listener notified when a {@link LocalRepoManager} is closed.
 * <p>
 * <b>Important:</b> This listener is <i>not notified about a proxy</i> being closed. It is only notified about
 * the real backend-instance being closed.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public interface LocalRepoManagerCloseListener extends EventListener {
	void preClose(LocalRepoManagerCloseEvent event);
	void postClose(LocalRepoManagerCloseEvent event);
}
