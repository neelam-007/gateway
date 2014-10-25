package com.l7tech.common.mime;

import com.l7tech.common.io.IOExceptionThrowingOutputStream;
import com.l7tech.util.*;

import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.internet.HeaderTokenizer;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeUtility;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Enumeration;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class MimeUtil {
    static final String PROP_UNICODE_MIME_VALUES = "com.l7tech.common.mime.values.utf8";
    static final boolean DEFAULT_UNICODE_MIME_VALUES = false;
    static boolean UNICODE_MIME_VALUES = SyspropUtil.getBoolean( PROP_UNICODE_MIME_VALUES, DEFAULT_UNICODE_MIME_VALUES );

    /** Encoding used by MIME headers.  Actually limited to 7-bit ASCII per RFC, but Latin1 (or UTF-8) is a safer choice. */
    static Charset ENCODING = MimeUtil.UNICODE_MIME_VALUES ? Charsets.UTF8 : Charsets.ISO8859;

    public static final String CONTENT_TYPE = "Content-Type";
    public static final String CONTENT_LENGTH = "Content-Length";
    public static final String CONTENT_ID = "Content-Id";
    public static final String CONTENT_TRANSFER_ENCODING = "Content-Transfer-Encoding";

    // TODO these should all be package private
    public static final String DEFAULT_ENCODING = "UTF-8";
    public static final String XML_VERSION = "1.0";
    public static final String MULTIPART_CONTENT_TYPE = "multipart/related";
    public static final String MULTIPART_TYPE = "type";
    public static final String MULTIPART_BOUNDARY = "boundary";
    public static final String MULTIPART_BOUNDARY_PREFIX = "--";
    public static final byte[] CRLF = "\r\n".getBytes();

    public static final String TRANSFER_ENCODING_BASE64 = "base64";
    public static final String TRANSFER_ENCODING_QUOTED_PRINTABLE = "quoted-printable";
    public static final String TRANSFER_ENCODING_7BIT = "7bit";
    public static final String TRANSFER_ENCODING_8BIT = "8bit";
    public static final String TRANSFER_ENCODING_BINARY = "binary";
    public static final String TRANSFER_ENCODING_DEFAULT = TRANSFER_ENCODING_7BIT;

    /**
     * Read a set of MIME headers and the delimiter line, and leave the InputStream positioned at the first byte
     * of the body content.
     *
     * @param stream an InputStream positioned at the first byte of a set of MIME headers.
     * @return the MimeHeaders that were parsed.
     * @throws IOException if the InputStream could not be read
     */
    public static MimeHeaders parseHeaders(InputStream stream) throws IOException {
        InternetHeaders headers;
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

            if ( UNICODE_MIME_VALUES ) {
                // SSG-9380: Treat (illegal) non-7BIT header values as UTF-8 rather than Latin-1
                value = new String( value.getBytes( Charsets.ISO8859 ), Charsets.UTF8 );
            }

            MimeHeader mh;
            if (name.equalsIgnoreCase(CONTENT_TYPE)) {
                // Special case for Content-Type: since it's such a big deal when doing multipart
                mh = ContentTypeHeader.create(value);
            } else {
                mh = MimeHeader.parseValue(name, value);
            }

            result.add(mh);
        }
        return result;
    }

    /**
     * Generates a random boundary consisting of between 3 and 40 legal characters,
     * plus a prefix of 0-5 hyphens and the magic quoted-printable-proof "=_"
     * @return a randomly-generated valid MIME boundary.  Never null.
     */
    public static byte[] randomBoundary() {
        StringBuffer bb = new StringBuffer();

        final int numDashes = RandomUtil.nextInt(5);
        for (int i = 0; i < numDashes; i++) {
            bb.append('-');
        }

        bb.append("=_");

        final int numPrintables = RandomUtil.nextInt(38) + 3;
        for (int i = 0; i < numPrintables; i++) {
            byte printable = (byte)(RandomUtil.nextInt(127-32)+32);
            if (HeaderTokenizer.MIME.indexOf(printable) < 0)
                bb.append(new String(new byte[] { printable }));
        }

        return bb.toString().getBytes();
    }

    /**
     * Construct a MIME multipart message body out of the specified boundary delimiter, array of part bodies,
     * and array of corresponding content types.
     *
     * @param boundary      the boundary to use.  Does not include leading "--".  Must not be null.
     * @param parts         an array of byte arrays.  May not be null or empty.  Individual byte arrays may be empty but not null.
     * @param contentTypes  an array of content types.  Must be the same length as parts.  May not be null.
     * @return a newly-constructed MIME multipart message body, starting with the opening delimiter and ending with a
     *         closing delimiter followed by an extra "--".
     */
    public static byte[] makeMultipartMessage(byte[] boundary, byte[][] parts, String[] contentTypes) {
        int numParts = parts.length;
        int size = 192 + (boundary.length * parts.length);
        for (int i = 0; i < parts.length; i++) {
            if (parts[i] != null) size += parts[i].length;
            if (contentTypes[i] != null) size += contentTypes[i].length();
        }

        if (size < 256) size = 256;
        try {
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
        } catch (IOException e) {
            throw new RuntimeException(e); // can't happen
        }
    }

    /**
     * Get an input stream that encodes the given data with the specified encoding.
     *
     * <p>If the encoding is not supported then the returned stream will throw an
     * exception on first use.</p>
     *
     * @param data The data to encode
     * @param contentTransferEncoding The encoding to use
     * @return The input stream
     */
    public static InputStream getEncodingInputStream( final byte[] data,
                                                      final String contentTransferEncoding ) {
        return getEncodingInputStream( data, 0, data.length, contentTransferEncoding );
    }

    /**
     * Get an input stream that encodes the given data with the specified encoding.
     *
     * <p>If the encoding is not supported then the returned stream will throw an
     * exception on first use.</p>
     *
     * @param data The data to encode
     * @param offset The offset for data
     * @param length The length of the data
     * @param contentTransferEncoding The encoding to use
     * @return The input stream
     */
    public static InputStream getEncodingInputStream( final byte[] data,
                                                      final int offset,
                                                      final int length,
                                                      final String contentTransferEncoding ) {
        // We need an input stream but MimeUtility only provides an
        // output stream so we'll convert on the fly.
        return IOUtils.toInputStream( data, offset, length, new Functions.Unary<OutputStream,OutputStream>(){
            @Override
            public OutputStream call( final OutputStream outputStream ) {
                OutputStream outStream;
                try {
                    outStream = MimeUtility.encode( outputStream, contentTransferEncoding );
                } catch ( MessagingException e ) {
                    outStream = new IOExceptionThrowingOutputStream(new IOException( ExceptionUtils.getMessage(e), e ));
                }

                return outStream;
            }
        });
    }

    /**
     * Get an input stream that decodes the given data with the specified encoding.
     *
     * @param in The data to decode
     * @param contentTransferEncoding The encoding to use
     * @return The input stream
     * @throws IOException If the specified encoding is not supported.
     */
    public static InputStream getDecodingInputStream( final InputStream in,
                                                      final String contentTransferEncoding ) throws IOException {
        try {
            return MimeUtility.decode( in, contentTransferEncoding );
        } catch ( MessagingException e ) {
            throw new IOException( ExceptionUtils.getMessage(e), e );
        }
    }

    /**
     * Is the given encoding a supported encoding that requires processing.
     *
     * @param contentTransferEncoding The encoding to check.
     * @return True if encoding/decoding is required.
     */
    public static boolean isProcessedTransferEncoding( final String contentTransferEncoding ) {
        return TRANSFER_ENCODING_BASE64.equals(contentTransferEncoding) ||
               TRANSFER_ENCODING_QUOTED_PRINTABLE.equals(contentTransferEncoding);
    }

    /**
     * Is the given encoding an identity encoding that does not require processing.
     *
     * @param contentTransferEncoding The encoding to check.
     * @return True if encoding/decoding is not required.
     */
    public static boolean isIdentityTransferEncoding( final String contentTransferEncoding ) {
        return TRANSFER_ENCODING_7BIT.equals(contentTransferEncoding) ||
               TRANSFER_ENCODING_8BIT.equals(contentTransferEncoding) ||
               TRANSFER_ENCODING_BINARY.equals(contentTransferEncoding);
    }

    static void updateMimeEncoding() {
        UNICODE_MIME_VALUES = SyspropUtil.getBoolean( PROP_UNICODE_MIME_VALUES, DEFAULT_UNICODE_MIME_VALUES );
        ENCODING = MimeUtil.UNICODE_MIME_VALUES ? Charsets.UTF8 : Charsets.ISO8859;
    }
}
