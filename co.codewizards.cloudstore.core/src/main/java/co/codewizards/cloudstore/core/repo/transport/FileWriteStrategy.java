package co.codewizards.cloudstore.core.repo.transport;

import co.codewizards.cloudstore.core.config.Config;

/**
 * Strategy controlling how and when the destination file is written.
 * <p>
 * This is merely a setting in the {@link Config}. The the actual implementation is in the
 * {@link co.codewizards.cloudstore.core.repo.transport.file.FileRepoTransport FileRepoTransport}.
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public enum FileWriteStrategy {
	/**
	 * Write directly into the destination file after all blocks have been transferred.
	 * During transfer, the destination file is not touched.
	 * <p>
	 * This strategy requires as much temporary space in the destination file system as
	 * blocks are transferred (in the worst case the entire file size).
	 */
	directAfterTransfer,

	/**
	 * Write each block directly into the destination file immediately when it was transferred.
	 * Don't wait for all other blocks.
	 * <p>
	 * In contrast to {@link #directAfterTransfer} this may leave the destination file in an
	 * inconsistent state for hours or even days - as long as the transfer takes.
	 */
	directDuringTransfer,

	/**
	 *
	 */
	replaceAfterTransfer
	;

	/**
	 * The {@code key} used with {@link Config#getPropertyAsEnum(String, Enum)}.
	 */
	public static final String CONFIG_KEY = "fileWriteStrategy"; //$NON-NLS-1$
	/**
	 * The {@code defaultValue} used with {@link Config#getPropertyAsEnum(String, Enum)}.
	 */
	public static final FileWriteStrategy CONFIG_DEFAULT_VALUE = directAfterTransfer;

}
