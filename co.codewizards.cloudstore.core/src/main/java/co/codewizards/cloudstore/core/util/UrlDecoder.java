package co.codewizards.cloudstore.core.util;

import static java.util.Objects.*;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;

/**
 * URL-decoder corresponding to {@link UrlEncoder}.
 * <p>
 * In contrast to the {@link java.net.URLDecoder URLDecoder}, this class therefore does <b>not</b> decode
 * '+' (plus) into ' ' (space)!
 * <p>
 * Additionally, this class does not use the default encoding, but always UTF-8, if not specified
 * otherwise.
 * <p>
 * The reason for this class is that {@link java.io.File#toURI() File.toURI()}
 * does not encode a "+" sign. Therefore, our URL-encoding and decoding must
 * not handle the "+" specifically.
 * <p>
 * Another reason is <a href="https://java.net/jira/browse/JERSEY-417">JERSEY-417</a>.
 * I originally used {@code org.glassfish.jersey.uri.UriComponent.encode(String, Type)}
 * at some code locations, but since not all code locations have a dependency on Jersey,
 * I decided to switch consistently everywhere to {@link UrlEncoder} and {@code UrlDecoder}.
 * <p>
 * This class was copied from {@link java.net.URLDecoder URLDecoder} and changed to fit our needs.
 * @see UrlEncoder
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public final class UrlDecoder {

	private UrlDecoder() {
	}

    /**
     * Decodes a {@code application/x-www-form-urlencoded} string using UTF-8.
     * @param s the {@code String} to decode
     * @return the newly decoded {@code String}
     * @see UrlEncoder#encode(String)
     */
	public static String decode(String s) {
        String str = decode(s, StandardCharsets.UTF_8);
        return str;
    }

    /**
     * Decodes a {@code application/x-www-form-urlencoded} string using a specific
     * encoding scheme.
     * The supplied encoding is used to determine
     * what characters are represented by any consecutive sequences of the
     * form "<i>{@code %xy}</i>".
     * <p>
     * <em><strong>Note:</strong> The <a href=
     * "http://www.w3.org/TR/html40/appendix/notes.html#non-ascii-chars">
     * World Wide Web Consortium Recommendation</a> states that
     * UTF-8 should be used. Not doing so may introduce
     * incompatibilities.</em>
     *
     * @param s the {@code String} to decode
     * @param enc   The name of a supported
     *    <a href="../lang/package-summary.html#charenc">character
     *    encoding</a>.
     * @return the newly decoded {@code String}
     * @exception  UnsupportedEncodingException
     *             If character encoding needs to be consulted, but
     *             named character encoding is not supported
     * @see UrlEncoder#encode(String, String)
     * @deprecated UTF-8 should be used; it is thus recommended to invoke {@link #decode(String)} instead.
     */
    @Deprecated
	public static String decode(String s, String enc) throws UnsupportedEncodingException {
    	requireNonNull(s, "s");
    	requireNonNull(enc, "enc");

    	Charset charset;
    	try {
            charset = Charset.forName(enc);
        } catch (IllegalCharsetNameException e) {
            throw new UnsupportedEncodingException(enc);
        } catch (UnsupportedCharsetException e) {
            throw new UnsupportedEncodingException(enc);
        }
    	return decode(s, charset);
    }

    /**
     * Decodes a {@code application/x-www-form-urlencoded} string using a specific
     * encoding scheme.
     * The supplied encoding is used to determine
     * what characters are represented by any consecutive sequences of the
     * form "<i>{@code %xy}</i>".
     * <p>
     * <em><strong>Note:</strong> The <a href=
     * "http://www.w3.org/TR/html40/appendix/notes.html#non-ascii-chars">
     * World Wide Web Consortium Recommendation</a> states that
     * UTF-8 should be used. Not doing so may introduce
     * incompatibilities.</em>
     *
     * @param s the {@code String} to decode
     * @param charset The <a href="../lang/package-summary.html#charenc">character encoding</a>.
     * @return the newly decoded {@code String}
     * @exception  UnsupportedEncodingException
     *             If character encoding needs to be consulted, but
     *             named character encoding is not supported
     * @see UrlEncoder#encode(String, Charset)
     * @deprecated UTF-8 should be used; it is thus recommended to invoke {@link #decode(String)} instead.
     */
    @Deprecated
	public static String decode(String s, Charset charset) {
    	requireNonNull(s, "s");
    	requireNonNull(charset, "charset");

        boolean needToChange = false;
        int numChars = s.length();
        StringBuffer sb = new StringBuffer(numChars > 500 ? numChars / 2 : numChars);
        int i = 0;

        char c;
        byte[] bytes = null;
        while (i < numChars) {
            c = s.charAt(i);
            switch (c) {
            case '%':
                /*
                 * Starting with this instance of %, process all
                 * consecutive substrings of the form %xy. Each
                 * substring %xy will yield a byte. Convert all
                 * consecutive  bytes obtained this way to whatever
                 * character(s) they represent in the provided
                 * encoding.
                 */

                try {

                    // (numChars-i)/3 is an upper bound for the number
                    // of remaining bytes
                    if (bytes == null)
                        bytes = new byte[(numChars-i)/3];
                    int pos = 0;

                    while ( ((i+2) < numChars) &&
                            (c=='%')) {
                        int v = Integer.parseInt(s.substring(i+1,i+3),16);
                        if (v < 0)
                            throw new IllegalArgumentException("URLDecoder: Illegal hex characters in escape (%) pattern - negative value");
                        bytes[pos++] = (byte) v;
                        i+= 3;
                        if (i < numChars)
                            c = s.charAt(i);
                    }

                    // A trailing, incomplete byte encoding such as
                    // "%x" will cause an exception to be thrown

                    if ((i < numChars) && (c=='%'))
                        throw new IllegalArgumentException(
                         "URLDecoder: Incomplete trailing escape (%) pattern");

                    sb.append(new String(bytes, 0, pos, charset));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(
                    "URLDecoder: Illegal hex characters in escape (%) pattern - "
                    + e.getMessage());
                }
                needToChange = true;
                break;
            default:
                sb.append(c);
                i++;
                break;
            }
        }

        return (needToChange? sb.toString() : s);
    }
}
