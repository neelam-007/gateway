package com.l7tech.common.mime;

import com.l7tech.common.util.CausedIOException;

import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.internet.InternetHeaders;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class MimeUtil {
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String CONTENT_LENGTH = "Content-Length";

    // TODO these should all be package private
    public static final String DEFAULT_ENCODING = "UTF-8";
    public static final String XML_VERSION = "1.0";
    public static final String MULTIPART_CONTENT_TYPE = "multipart/related";
    public static final String MULTIPART_TYPE = "type";
    public static final String MULTIPART_BOUNDARY = "boundary";
    public static final String MULTIPART_BOUNDARY_PREFIX = "--";
    public static final String CONTENT_ID = "Content-Id";

    static public void addModifiedSoapPart(StringBuffer sbuf, String modifiedSoapEnvelope, PartInfo part, String boundary) {

        if(sbuf == null) throw new IllegalArgumentException("The StringBuffer is NULL");
        if(modifiedSoapEnvelope == null) throw new IllegalArgumentException("The modified SOAP envelope is NULL");
        if(part == null) throw new IllegalArgumentException("The SOAP part is NULL");
        if(boundary == null) throw new IllegalArgumentException("The StringBuffer is NULL");

        sbuf.append(MULTIPART_BOUNDARY_PREFIX + boundary + "\r\n");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MimeHeaders headers = part.getHeaders();
        try {
            headers.write(baos);
            sbuf.append(new String(baos.toByteArray(), "UTF-8"));
        } catch (IOException e) {
            throw new RuntimeException(e); // can't happen, it's a baos
        }
        sbuf.append(modifiedSoapEnvelope);
        sbuf.append("\r\n" + MULTIPART_BOUNDARY_PREFIX + boundary + "\r\n");
    }

    /**
     * Read a set of MIME headers and the delimiter line, and leave the InputStream positioned at the first byte
     * of the body content.
     *
     * @param stream an InputStream positioned at the first byte of a set of MIME headers.
     * @return the MimeHeaders that were parsed.
     * @throws IOException if the InputStream could not be read
     */
    public static MimeHeaders parseHeaders(InputStream stream) throws IOException {
        InternetHeaders headers = null;
        try {
            headers = new InternetHeaders(stream);
        } catch (MessagingException e) {
            throw new CausedIOException("Coudln't parse MIME headers", e);
        }

        MimeHeaders result = new MimeHeaders();
        for (Enumeration e = headers.getAllHeaders(); e.hasMoreElements(); ) {
            Header h = (Header)e.nextElement();
            String name = h.getName();
            String value = h.getValue();
            MimeHeader mh;
            if (name.equalsIgnoreCase(CONTENT_TYPE)) {
                // Special case for Content-Type: since it's such a big deal when doing multipart
                mh = ContentTypeHeader.parseValue(value);
            } else {
                mh = MimeHeader.parseValue(name, value);
            }

            result.add(mh);
        }
        return result;
    }

    static public String unquote( String value ) throws IOException {

        if(value == null) return value;

        if (value.startsWith("\"")) {
            if (value.endsWith("\"")) {
                value = value.substring(1,value.length()-1);
            } else throw new IOException("Invalid header format (mismatched quotes in value)");
        }
        return value;
    }

    static public String removeConentIdBrackets(String value) throws IOException {

        if(value == null) return value;

        if (value.startsWith("<")) {
            if (value.endsWith(">")) {
                value = value.substring(1,value.length()-1);
            } else throw new IOException("Invalid Content Id format (mismatched brackets in value)");
        }
        return value;
    }

    public static MimeHeader parseHeader(String line) {
        return null;
    }
}
