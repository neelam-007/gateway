package com.l7tech.common.mime;

import com.l7tech.common.util.CausedIOException;

import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.internet.HeaderTokenizer;
import javax.mail.internet.InternetHeaders;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
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
    private static SecureRandom random = new SecureRandom();
    public static final byte[] CRLF = "\r\n".getBytes();

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
            throw new CausedIOException("Couldn't parse MIME headers", e);
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

    /**
     * Generates a random boundary consisting of between 1 and 40 legal characters,
     * plus a prefix of 0-5 hyphens and the magic quoted-printable-proof "=_"
     * @return
     */
    public static byte[] randomBoundary() {
        StringBuffer bb = new StringBuffer();

        for (int i = 0; i < random.nextInt(5); i++) {
            bb.append('-');
        }

        bb.append("=_");

        for (int i = 0; i < random.nextInt(40)+1; i++) {
            byte printable = (byte)(random.nextInt(127-32)+32);
            if (HeaderTokenizer.MIME.indexOf(printable) < 0)
                bb.append(new String(new byte[] { printable }));
        }

        return bb.toString().getBytes();
    }

    public static byte[] makeMultipartMessage(byte[] boundary ,byte[][] parts, String[] contentTypes) throws IOException {
        int numParts = parts.length;
        int size = 192 + (boundary.length * parts.length);
        for (int i = 0; i < parts.length; i++) {
            if (parts[i] != null) size += parts[i].length;
            if (contentTypes[i] != null) size += contentTypes[i].length();
        }

        if (size < 256) size = 256;
        ByteArrayOutputStream baos = new ByteArrayOutputStream(size);
        baos.write(("Content-Type: multipart/related; boundary=\"" + new String(boundary) + "\"").getBytes());
        baos.write(CRLF);
        baos.write(CRLF);

        for (int i = 0; i < numParts; i++) {
            baos.write(CRLF);
            baos.write("--".getBytes());
            baos.write(boundary);
            baos.write(CRLF);

            baos.write(("Content-Length: " + parts[i].length).getBytes());
            baos.write(CRLF);
            baos.write(("Content-Type: " + contentTypes[i]).getBytes());
            baos.write(CRLF);
            baos.write(CRLF);

            baos.write(parts[i]);
        }

        baos.write(CRLF);
        baos.write("--".getBytes());
        baos.write(boundary);
        baos.write("--".getBytes());
        baos.write(CRLF);
        return baos.toByteArray();
    }
}
