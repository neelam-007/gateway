/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.http;

import com.l7tech.common.io.ByteLimitInputStream;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.util.Charsets;
import com.l7tech.util.IOUtils;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides a client-independant, generic interface to an HTTP response.
 */
public abstract class GenericHttpResponse implements Closeable, GenericHttpResponseParams {
    protected static class GuessedEncodingResult {
        public Charset encoding;
        public int bytesToSkip;
    }
    
    /**
     * Get the InputStream from which the response body can be read.  Some implementations may close the
     * entire HTTP response when this InputStream is closed.
     *
     * @return the InputStream of the response body.  Never null.
     * @throws GenericHttpException if there is a network problem or protocol violation.
     */
    public abstract InputStream getInputStream() throws GenericHttpException;

    /**
     * Get the entire HTTP response body as a String, if the returned HTTP status was 200.
     *
     * @param isXml           Whether the input stream contains an XML document or not. If true, then this
     *                        method will try to get the charset encoding from the input stream rather than the HTTP
     *                        headers.
     * @param maxResponseSize int maximum allowed size of the response in bytes
     * @return a String with HTTP-ly-correct encoding (default ISO8859-1 if not declared).  Never null.
     * @throws IOException if the status isn't 200
     * @throws java.io.UnsupportedEncodingException
     *                     if we can't handle the declared character encoding
     */
    public String getAsString(boolean isXml, int maxResponseSize) throws IOException {
        if (getStatus() != HttpConstants.STATUS_OK)
            throw new IOException("HTTP status was " + getStatus());

        byte[] bytes = IOUtils.slurpStream(new ByteLimitInputStream(getInputStream(), 1024, maxResponseSize));

        // Try to get the charset encoding from the data, if that fails, then use
        // HTTP charset encoding.
        ContentTypeHeader ctype = getContentType();
        if (isXml) {
            GuessedEncodingResult result = getXmlEncoding(bytes);
            if (result.encoding == null) {
                result.encoding = ctype == null ? ContentTypeHeader.DEFAULT_HTTP_ENCODING : ctype.getEncoding();
                result.bytesToSkip = 0;
            }
            return new String(bytes, result.bytesToSkip, bytes.length - result.bytesToSkip, result.encoding);
        } else {
            Charset encoding = ctype == null ? ContentTypeHeader.DEFAULT_HTTP_ENCODING : ctype.getEncoding();
            return new String(bytes, encoding);
        }
    }

    /**
     * Tries to determine the charset for the supplied XML byte array.
     *
     * The following charsets are known to fail to be recognized by this method. They are still supported,
     * but only if the proper encoding was specified in the Content-type header.
     * <b>JIS_X0212-1990, IBM290, x-IBM300, x-IBM834, x-IBM930, x-JIS0208, x-MacDingbat, x-MacSymbol</b>
     * @param bytes The byte array to examine
     * @return The charset encoding if it could be determined, or null
     */
    protected static GuessedEncodingResult getXmlEncoding(byte[] bytes) {
        GuessedEncodingResult result = new GuessedEncodingResult();

        if(bytes.length < 4) {
            return result;
        }

        if(bytes[0] == (byte)0x00 && bytes[1] == (byte)0x00) {
            if(bytes[2] == (byte)0xfe && bytes[3] == (byte)0xff) {
                result.encoding = Charsets.UTF32BE;
            } else if(bytes[2] == (byte)0x00 && bytes[3] == (byte)0x3c) {
                result.encoding = getDeclaredEncoding(bytes, Charsets.UTF32BE);
            }
        } else if(bytes[0] == (byte)0xff && bytes[1] == (byte)0xfe) {
            if(bytes[2] == (byte)0x00 && bytes[3] == (byte)0x00) {
                result.encoding = Charsets.UTF32LE;
            } else {
                result.encoding = Charsets.X_UTF16LE_BOM;
            }
        } else if(bytes[0] == (byte)0xfe && bytes[1] == (byte)0xff) {
            result.encoding = Charsets.UTF16BE;
            result.bytesToSkip = 2;
        } else if(bytes[0] == (byte)0xef && bytes[1] == (byte)0xbb && bytes[2] == (byte)0xbf) {
            result.encoding = Charsets.UTF8;
            result.bytesToSkip = 3;
        } else if(bytes[0] == (byte)0x3c && bytes[1] == (byte)0x00) {
            if(bytes[2] == (byte)0x00 && bytes[3] == (byte)0x00) {
                result.encoding = getDeclaredEncoding(bytes, Charsets.UTF32LE);
            } else if(bytes[2] == (byte)0x3f && bytes[3] == (byte)0x00) {
                result.encoding = getDeclaredEncoding(bytes, Charsets.UTF16LE);
            }
        } else if(bytes[0] == (byte)0x00 && bytes[1] == (byte)0x3c && bytes[2] == (byte)0x00 && bytes[3] == (byte)0x3f) {
            result.encoding = getDeclaredEncoding(bytes, Charsets.UTF16BE);
        } else if(bytes[0] == (byte)0x3c && bytes[1] == (byte)0x3f && bytes[2] == (byte)0x78 && bytes[3] == (byte)0x6d) {
            result.encoding = getDeclaredEncoding(bytes, Charsets.UTF8);
        } else if(bytes[0] == (byte)0x4c && bytes[1] == (byte)0x6f && bytes[2] == (byte)0xa7 && bytes[3] == (byte)0x94 && Charset.isSupported("Cp1047")) {
            result.encoding = getDeclaredEncoding(bytes, Charset.forName("Cp1047"));
        }

        return result;
    }

    private static final Pattern FIND_ENCODING =
            Pattern.compile("^<\\?xml\\s+version\\s*=\\s*(.).*\\1\\s+encoding\\s*=\\s*(.)(.*?)\\2\\s*(?:\\s+standalone\\s*=\\s*(.).*\\4\\s*)?\\?>", Pattern.MULTILINE);

    /**
     * Examines the supplied byte array looking for an XML declaration. If an XML declaration is found
     * and it contains the encoding attribute, then return the value of the encoding attribute.
     * @param bytes The byte array to examine
     * @param possibleEncoding The charset to assume that the byte array is encoded with for the purposes
     * of finding the XML declaration
     * @return The value of the encoding attribute of the XML declaration if found; <CODE>possibleEncoding</CODE>
     *         if the value in the declaration is unrecognized; or null if no declaration is found.
     */
    private static Charset getDeclaredEncoding(byte[] bytes, Charset possibleEncoding) {
        try {
            // Only look for the XML declaration in the first 1kB
            String xmlString = new String(bytes, 0, 1024 > bytes.length ? bytes.length : 1024, possibleEncoding);
            // With some charsets, our guess at this point could have messed up the quotes, so we need to find what
            // character is used for quotes
            Matcher matcher = FIND_ENCODING.matcher(xmlString);
            if(matcher.find()) {
                return Charset.forName(matcher.group(3));
            } else {
                // There isn't an XML declaration, or it doesn't specify the encoding, so fallback to using
                // the HTTP "Content-type" header.
                return null;
            }
        } catch(UnsupportedCharsetException e) {
            return possibleEncoding;
        }
    }

    /**
     * Free all resources used by this response.
     * <p>
     * After close() has been called, the behaviour of this class is not defined.  Any InputStream obtained
     * from {@link #getInputStream} should no longer be used.
     * <p>
     * However, any {@link HttpHeaders} object returned from {@link #getHeaders()} is guaranteed to remain useable even
     * after this request has been closed.    
     * <p>
     * Might throw unavoidable Errors (ie, OutOfMemoryError), but will never throw runtime exceptions.
     */
    public abstract void close();
}
