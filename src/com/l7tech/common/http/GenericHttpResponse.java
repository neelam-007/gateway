/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.http;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.util.HexUtils;

import java.io.InputStream;
import java.io.IOException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Provides a client-independant, generic interface to an HTTP response.
 */
public abstract class GenericHttpResponse implements GenericHttpResponseParams {
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
     * @param isXml Whether the input stream contains an XML document or not. If true, then this
     * method will try to get the charset encoding from the input stream rather than the HTTP
     * headers.
     * @return a String with HTTP-ly-correct encoding (default ISO8859-1 if not declared).  Never null.
     * @throws IOException  if the status isn't 200
     * @throws java.io.UnsupportedEncodingException if we can't handle the declared character encoding
     */
    public String getAsString(boolean isXml) throws IOException {
        if (getStatus() != HttpConstants.STATUS_OK)
            throw new IOException("HTTP status was " + getStatus());

        byte[] bytes = HexUtils.slurpStreamLocalBuffer(getInputStream());

        // Try to get the charset encoding from the data, if that fails, then use
        // HTTP charset encoding.
        ContentTypeHeader ctype = getContentType();
        String encoding = isXml ? getXmlEncoding(bytes) : null;
        if(encoding == null) {
            encoding = ctype == null ? ContentTypeHeader.DEFAULT_HTTP_ENCODING : ctype.getEncoding();
        }
        return new String(bytes, encoding);
    }

    /**
     * Tries to determine the charset for the supplied XML byte array.
     * @param bytes The byte array to examine
     * @return The charset encoding if it could be determined, or null
     */
    private static String getXmlEncoding(byte[] bytes) {
        if(bytes.length < 4) {
            return null;
        }

        if(bytes[0] == (byte)0x00 && bytes[1] == (byte)0x00) {
            if(bytes[2] == (byte)0xfe && bytes[3] == (byte)0xff) {
                return "UTF-32BE";
            } else if(bytes[2] == (byte)0x00 && bytes[3] == (byte)0x3c) {
                return getDeclaredEncoding(bytes, "UTF32-BE");
            }
        } else if(bytes[0] == (byte)0xff && bytes[1] == (byte)0xfe) {
            if(bytes[2] == (byte)0x00 && bytes[3] == (byte)0x00) {
                return "UTF-32LE";
            } else {
                return "UTF-16LE";
            }
        } else if(bytes[0] == (byte)0xfe && bytes[1] == (byte)0xff) {
            return "UTF-16BE";
        } else if(bytes[0] == (byte)0xef && bytes[1] == (byte)0xbb && bytes[2] == (byte)0xbf) {
            return "UTF-8";
        } else if(bytes[0] == (byte)0x3c && bytes[1] == (byte)0x00) {
            if(bytes[2] == (byte)0x00 && bytes[3] == (byte)0x00) {
                return getDeclaredEncoding(bytes, "UTF32-LE");
            } else if(bytes[2] == (byte)0x3f && bytes[3] == (byte)0x00) {
                return getDeclaredEncoding(bytes, "UTF-16LE");
            }
        } else if(bytes[0] == (byte)0x00 && bytes[1] == (byte)0x3c && bytes[2] == (byte)0x00 && bytes[3] == (byte)0x3f) {
            return getDeclaredEncoding(bytes, "UTF-16BE");
        } else if(bytes[0] == (byte)0x3c && bytes[1] == (byte)0x3f && bytes[2] == (byte)0x78 && bytes[3] == (byte)0x6d) {
            return getDeclaredEncoding(bytes, "UTF-8");
        } else if(bytes[0] == (byte)0x4c && bytes[1] == (byte)0x6f && bytes[2] == (byte)0xa7 && bytes[3] == (byte)0x94) {
            return getDeclaredEncoding(bytes, "Cp1047");
        }

        return null;
    }

    /**
     * Examines the supplied byte array looking for an XML declaration. If an XML declaration is found
     * and it contains the encoding attribute, then return the value of the encoding attribute.
     * @param bytes The byte array to examine
     * @param possibleEncoding The charset to assume that the byte array is encoded with for the purposes
     * of finding the XML declaration
     * @return The value of the encoding attribute of the XML declaration if found, or <CODE>possibleEncoding</CODE>
     */
    private static String getDeclaredEncoding(byte[] bytes, String possibleEncoding) {
        try {
            String xmlString = new String(bytes, possibleEncoding);
            Pattern pattern = Pattern.compile("<?\\s*xml\\s+version=\"[^\"]+\"\\s+encoding=\"([^\"]+)\"\\s*?>", Pattern.MULTILINE);
            Matcher matcher = pattern.matcher(xmlString);
            if(matcher.matches()) {
                return matcher.group(1);
            } else {
                return possibleEncoding;
            }
        } catch(java.io.UnsupportedEncodingException e) {
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
