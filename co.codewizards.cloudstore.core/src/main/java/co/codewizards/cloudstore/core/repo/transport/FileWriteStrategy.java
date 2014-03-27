package co.codewizards.cloudstore.core.repo.transport;

import co.codewizards.cloudstore.core.config.Config;

/**
 * Strategy controlling how and when the destination file is written.
 * <p>
 * This is merely a setting in the {@link Config}. The actual implementation is in the
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
	 * blocks are transferred: In the worst case the total space requirement is thus twice
	 * the file size (old file + all blocks).
	 */
	directAfterTransfer,

	/**
	 * Write each block directly into the destination file immediately when it was transferred.
	 * Don't wait for all other blocks.
	 * <p>
	 * In contrast to {@link #directAfterTransfer} this may leave the destination file in an
	 * inconsistent state for hours or even days - as long as the transfer takes.
	 * <p>
	 * However, this strategy requires the least space in the file system: Only once the file size.
	 * There are no temporary files involved and thus no temporary space required.
	 */
	directDuringTransfer,

	/**
	 * Same as {@link #directAfterTransfer}, but write a completely new file and then switch
	 * the files (delete the old file and rename the new file).
	 * <p>
	 * This strategy is the safest concerning consistency, but requires the most temporary space in
	 * the destination file system: In the worst case the total space requirement is thus 3 times
	 * the file size (old file + all blocks + new file).
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
