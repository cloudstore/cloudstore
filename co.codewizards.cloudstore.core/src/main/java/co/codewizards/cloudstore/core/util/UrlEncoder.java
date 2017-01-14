package co.codewizards.cloudstore.core.util;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.io.CharArrayWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.BitSet;

/**
 * URL-encoder encoding all special characters (that cannot be left unchanged) as "%...".
 * <p>
 * In contrast to the {@link java.net.URLEncoder URLEncoder}, this class does <b>not</b> encode
 * ' ' (space) space as '+' (plus)!
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
 * I decided to switch consistently everywhere to {@code UrlEncoder} and {@link UrlDecoder}.
 * <p>
 * This class was copied from {@link java.net.URLEncoder URLEncoder} and changed to fit our needs.
 * @see UrlDecoder
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public final class UrlEncoder {

	private UrlEncoder() {
	}

    static BitSet dontNeedEncoding;
    static final int caseDiff = ('a' - 'A');

    static {

        /* The list of characters that are not encoded has been
         * determined as follows:
         *
         * RFC 2396 states:
         * -----
         * Data characters that are allowed in a URI but do not have a
         * reserved purpose are called unreserved.  These include upper
         * and lower case letters, decimal digits, and a limited set of
         * punctuation marks and symbols.
         *
         * unreserved  = alphanum | mark
         *
         * mark        = "-" | "_" | "." | "!" | "~" | "*" | "'" | "(" | ")"
         *
         * Unreserved characters can be escaped without changing the
         * semantics of the URI, but this should not be done unless the
         * URI is being used in a context that does not allow the
         * unescaped character to appear.
         * -----
         *
         * It appears that both Netscape and Internet Explorer escape
         * all special characters from this list with the exception
         * of "-", "_", ".", "*". While it is not clear why they are
         * escaping the other characters, perhaps it is safest to
         * assume that there might be contexts in which the others
         * are unsafe if not escaped. Therefore, we will use the same
         * list. It is also noteworthy that this is consistent with
         * O'Reilly's "HTML: The Definitive Guide" (page 164).
         *
         * As a last note, Intenet Explorer does not encode the "@"
         * character which is clearly not unreserved according to the
         * RFC. We are being consistent with the RFC in this matter,
         * as is Netscape.
         *
         */

        dontNeedEncoding = new BitSet(256);
        int i;
        for (i = 'a'; i <= 'z'; i++) {
            dontNeedEncoding.set(i);
        }
        for (i = 'A'; i <= 'Z'; i++) {
            dontNeedEncoding.set(i);
        }
        for (i = '0'; i <= '9'; i++) {
            dontNeedEncoding.set(i);
        }
        dontNeedEncoding.set('-');
        dontNeedEncoding.set('_');
        dontNeedEncoding.set('.');
        dontNeedEncoding.set('*');
    }


    /**
     * Translates a string into {@code application/x-www-form-urlencoded}
     * format using UTF-8.
     * @param   s   {@code String} to be translated.
     */
    public static String encode(String s) {
        String str = encode(s, StandardCharsets.UTF_8);
        return str;
    }

    /**
     * Translates a string into {@code application/x-www-form-urlencoded}
     * format using a specific encoding scheme. This method uses the
     * supplied encoding scheme to obtain the bytes for unsafe
     * characters.
     * <p>
     * <em><strong>Note:</strong> The <a href=
     * "http://www.w3.org/TR/html40/appendix/notes.html#non-ascii-chars">
     * World Wide Web Consortium Recommendation</a> states that
     * UTF-8 should be used. Not doing so may introduce
     * incompatibilities.</em>
     *
     * @param   s   {@code String} to be translated.
     * @param   enc   The name of a supported
     *    <a href="../lang/package-summary.html#charenc">character
     *    encoding</a>.
     * @return  the translated {@code String}.
     * @exception  UnsupportedEncodingException
     *             If the named encoding is not supported
     * @see UrlDecoder#decode(String, String)
     * @deprecated UTF-8 should be used; it is thus recommended to invoke {@link #encode(String)} instead.
     */
    @Deprecated
	public static String encode(String s, String enc) throws UnsupportedEncodingException {
    	assertNotNull(s, "s");
    	assertNotNull(enc, "enc");
    	Charset charset;
    	try {
            charset = Charset.forName(enc);
        } catch (IllegalCharsetNameException e) {
            throw new UnsupportedEncodingException(enc);
        } catch (UnsupportedCharsetException e) {
            throw new UnsupportedEncodingException(enc);
        }
    	return encode(s, charset);
    }

    /**
     * Translates a string into {@code application/x-www-form-urlencoded}
     * format using a specific encoding scheme. This method uses the
     * supplied encoding scheme to obtain the bytes for unsafe
     * characters.
     * <p>
     * <em><strong>Note:</strong> The <a href=
     * "http://www.w3.org/TR/html40/appendix/notes.html#non-ascii-chars">
     * World Wide Web Consortium Recommendation</a> states that
     * UTF-8 should be used. Not doing so may introduce
     * incompatibilities.</em>
     *
     * @param   s   {@code String} to be translated.
     * @param   charset   The <a href="../lang/package-summary.html#charenc">character encoding</a>.
     * @return  the translated {@code String}.
     * @exception  UnsupportedEncodingException
     *             If the named encoding is not supported
     * @see UrlDecoder#decode(String, Charset)
     * @deprecated UTF-8 should be used; it is thus recommended to invoke {@link #encode(String)} instead.
     */
    @Deprecated
	public static String encode(String s, Charset charset) {
    	assertNotNull(s, "s");
    	assertNotNull(charset, "charset");

        boolean needToChange = false;
        StringBuffer out = new StringBuffer(s.length());
        CharArrayWriter charArrayWriter = new CharArrayWriter();

        for (int i = 0; i < s.length();) {
            int c = s.charAt(i);
            //System.out.println("Examining character: " + c);
            if (dontNeedEncoding.get(c)) {
                //System.out.println("Storing: " + c);
                out.append((char)c);
                i++;
            } else {
                // convert to external encoding before hex conversion
                do {
                    charArrayWriter.write(c);
                    /*
                     * If this character represents the start of a Unicode
                     * surrogate pair, then pass in two characters. It's not
                     * clear what should be done if a bytes reserved in the
                     * surrogate pairs range occurs outside of a legal
                     * surrogate pair. For now, just treat it as if it were
                     * any other character.
                     */
                    if (c >= 0xD800 && c <= 0xDBFF) {
                        /*
                          System.out.println(Integer.toHexString(c)
                          + " is high surrogate");
                        */
                        if ( (i+1) < s.length()) {
                            int d = s.charAt(i+1);
                            /*
                              System.out.println("\tExamining "
                              + Integer.toHexString(d));
                            */
                            if (d >= 0xDC00 && d <= 0xDFFF) {
                                /*
                                  System.out.println("\t"
                                  + Integer.toHexString(d)
                                  + " is low surrogate");
                                */
                                charArrayWriter.write(d);
                                i++;
                            }
                        }
                    }
                    i++;
                } while (i < s.length() && !dontNeedEncoding.get((c = s.charAt(i))));

                charArrayWriter.flush();
                String str = new String(charArrayWriter.toCharArray());
                byte[] ba = str.getBytes(charset);
                for (int j = 0; j < ba.length; j++) {
                    out.append('%');
                    char ch = Character.forDigit((ba[j] >> 4) & 0xF, 16);
                    // converting to use uppercase letter as part of
                    // the hex value if ch is a letter.
                    if (Character.isLetter(ch)) {
                        ch -= caseDiff;
                    }
                    out.append(ch);
                    ch = Character.forDigit(ba[j] & 0xF, 16);
                    if (Character.isLetter(ch)) {
                        ch -= caseDiff;
                    }
                    out.append(ch);
                }
                charArrayWriter.reset();
                needToChange = true;
            }
        }
        return (needToChange? out.toString() : s);
    }
}
