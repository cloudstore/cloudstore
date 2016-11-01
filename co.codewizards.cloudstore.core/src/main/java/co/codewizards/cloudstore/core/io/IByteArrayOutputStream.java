package co.codewizards.cloudstore.core.io;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * {@link java.io.ByteArrayOutputStream ByteArrayOutputStream}-representing interface to be used in API contracts.
 * <p>
 * See {@link IOutputStream} for further details.
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public interface IByteArrayOutputStream extends IOutputStream {

	/**
     * Writes the complete contents of this byte array output stream to
     * the specified output stream argument, as if by calling the output
     * stream's write method using <code>out.write(buf, 0, count)</code>.
     *
     * @param      out   the output stream to which to write the data.
     * @exception  IOException  if an I/O error occurs.
     */
    void writeTo(IOutputStream out) throws IOException;

    /**
     * Resets the <code>count</code> field of this byte array output
     * stream to zero, so that all currently accumulated output in the
     * output stream is discarded. The output stream can be used again,
     * reusing the already allocated buffer space.
     *
     * @see     java.io.ByteArrayInputStream#count
     */
    void reset();

    /**
     * Creates a newly allocated byte array. Its size is the current
     * size of this output stream and the valid contents of the buffer
     * have been copied into it.
     *
     * @return  the current contents of this output stream, as a byte array.
     * @see     java.io.ByteArrayOutputStream#size()
     */
    byte[] toByteArray();

    /**
     * Returns the current size of the buffer.
     *
     * @return  the value of the <code>count</code> field, which is the number
     *          of valid bytes in this output stream.
     * @see     java.io.ByteArrayOutputStream#count
     */
    int size();

//    /**
//     * Converts the buffer's contents into a string decoding bytes using the
//     * platform's default character set. The length of the new <tt>String</tt>
//     * is a function of the character set, and hence may not be equal to the
//     * size of the buffer.
//     *
//     * <p> This method always replaces malformed-input and unmappable-character
//     * sequences with the default replacement string for the platform's
//     * default character set. The {@linkplain java.nio.charset.CharsetDecoder}
//     * class should be used when more control over the decoding process is
//     * required.
//     *
//     * @return String decoded from the buffer's contents.
//     * @since  JDK1.1
//     */
//    public synchronized String toString() {
//        return new String(buf, 0, count);
//    }

    /**
     * Converts the buffer's contents into a string by decoding the bytes using
     * the named {@link java.nio.charset.Charset charset}. The length of the new
     * <tt>String</tt> is a function of the charset, and hence may not be equal
     * to the length of the byte array.
     *
     * <p> This method always replaces malformed-input and unmappable-character
     * sequences with this charset's default replacement string. The {@link
     * java.nio.charset.CharsetDecoder} class should be used when more control
     * over the decoding process is required.
     *
     * @param      charsetName  the name of a supported
     *             {@link java.nio.charset.Charset charset}
     * @return     String decoded from the buffer's contents.
     * @exception  UnsupportedEncodingException
     *             If the named charset is not supported
     * @since      JDK1.1
     */
    String toString(String charsetName) throws UnsupportedEncodingException;
}