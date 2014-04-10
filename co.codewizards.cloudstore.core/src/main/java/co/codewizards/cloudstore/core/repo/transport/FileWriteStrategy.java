package co.codewizards.cloudstore.core.repo.transport;

import co.codewizards.cloudstore.core.config.Config;

/**
 * Strategy controlling how and when a destination file is written.
 * <p>
 * This is merely a setting in the {@link Config}. The actual implementation is in the
 * {@link co.codewizards.cloudstore.local.transport.file.FileRepoTransport FileRepoTransport}.
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public enum FileWriteStrategy {
	/**
	 * Write directly into the destination file after all blocks have been transferred.
	 * During transfer, the destination file is not touched.
	 * <p>
	 * This strategy requires as much temporary space in the destination file system as
	 * blocks are transferred: The maximum total space requirement is thus twice
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
	 * There are no temporary files involved and thus no additional temporary space required.
	 */
	directDuringTransfer,

	/**
	 * Similar to {@link #directAfterTransfer}, but write a new file and then switch
	 * the files (delete the old file and rename the new file).
	 * <p>
	 * This strategy is the safest concerning consistency, but requires the most temporary space in
	 * the destination file system: The maximum total space requirement is a bit more than twice
	 * the file size (old file + blocks not yet written to new file + partial new file).
	 * Because the blocks are immediately deleted when written to the (partial) new file
	 * and the new file is growing while blocks are deleted (it doesn't have the final size immediately),
	 * the required space is <i>not</i> 3 times the size, but - as said - only a bit more than twice
	 * the size.
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
