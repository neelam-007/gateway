/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.mime;

import com.l7tech.common.util.CausedIOException;

import javax.mail.internet.HeaderTokenizer;
import javax.mail.internet.MimeUtility;
import javax.mail.internet.ParseException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

/**
 * Represents a MIME Content-Type header.
 */
public class ContentTypeHeader extends MimeHeader {
    private static final Logger logger = Logger.getLogger(ContentTypeHeader.class.getName());
    public static final ContentTypeHeader OCTET_STREAM_DEFAULT; // application/octet-stream
    public static final ContentTypeHeader TEXT_DEFAULT; // text/plain; charset=UTF-8
    public static final ContentTypeHeader XML_DEFAULT; // text/xml; charset=UTF-8
    public static final String CHARSET = "charset";
    public static final String DEFAULT_CHARSET_MIME = "utf-8";

    static {
        try {
            OCTET_STREAM_DEFAULT = parseValue("application/octet-stream");
            OCTET_STREAM_DEFAULT.getEncoding();
            TEXT_DEFAULT = parseValue("text/plain; charset=UTF-8");
            XML_DEFAULT = parseValue("text/xml; charset=UTF-8");
            XML_DEFAULT.getEncoding();
        } catch (Throwable e) {
            throw new Error(e);
        }
    }

    private final String type;
    private final String subtype;

    private String javaEncoding = null; // figured out lazy-like
    private String mimeCharset = null;

    /**
     * Create a new ContentTypeHeader with the specified type, subtype, and parameters.  Currently
     * if the type is "multipart", the subtype may only be "related".
     *
     * @param type   the major type, ie "text". may not be null
     * @param subtype the minor type, ie "xml". may not be null
     * @param params the parameters, ie {charset=>"utf-8"}.  might be null
     * @throws IllegalArgumentException if type is multipart, but boundary param is missing or empty
     * @throws IllegalArgumentException if type is multipart, but the subtype is other than "related"
     */
    ContentTypeHeader(String type, String subtype, Map params) throws IOException {
        super(MimeUtil.CONTENT_TYPE, type + "/" + subtype, params);
        this.type = type;
        this.subtype = subtype;

        if ("multipart".equalsIgnoreCase(type)) {
            String boundary = (String)this.params.get("boundary");
            if (boundary == null || boundary.length() < 1)
                throw new IOException("Content-Type of type multipart must include a boundary parameter (RFC 2045 sec 5)");
            byte[] bytes = boundary.getBytes(ENCODING);
            for (int i = 0; i < bytes.length; ++i) {
                if (bytes[i] < ' ' || bytes[i] > 126)
                    throw new IOException("Content-Type multipart boundary contains illegal character; must be US-ASCII (RFC 2045)");
            }

        }
    }

    /**
     * Parse a MIME Content-Type: header, not including the header name and colon.
     * Example: <code>{@link #parseValue}("text/html; charset=\"UTF-8\"")</code>
     *
     * @param contentTypeHeaderValue the header value to parse
     * @return a ContentTypeHeader instance.  Never null.
     * @throws java.io.IOException  if the specified header value was missing, empty, or syntactically invalid
     */
    public static ContentTypeHeader parseValue(String contentTypeHeaderValue) throws IOException {
        if (contentTypeHeaderValue == null || contentTypeHeaderValue.length() < 1)
            throw new IOException("MIME Content-Type header missing or empty");
        HeaderTokenizer ht = new HeaderTokenizer(contentTypeHeaderValue, HeaderTokenizer.MIME, true);
        HeaderTokenizer.Token token;
        try {
            // Get type
            token = ht.next();
            if (token.getType() == HeaderTokenizer.Token.EOF)
                throw new IOException("MIME Content-Type type missing");
            if (token.getType() != HeaderTokenizer.Token.ATOM)
                throw new IOException("MIME Content-Type supertype is not an atom: " + token.getValue());

            String type = token.getValue();

            // Eat slash
            token = ht.next();
            if (token.getType() != '/')
                throw new IOException("MIME Content-Type supertype is not followed by a slash: " + token.getValue());

            // Get subtype
            token = ht.next();
            if (token.getType() == HeaderTokenizer.Token.EOF)
                throw new IOException("MIME Content-Type subtype missing");
            if (token.getType() != HeaderTokenizer.Token.ATOM)
                throw new IOException("MIME Content-Type subtype is not an atom: " + token.getValue());
            String subtype = token.getValue();

            // Check for parameters
            Map params = new TreeMap(String.CASE_INSENSITIVE_ORDER);
            for (;;) {
                token = ht.next();

                if (token.getType() == HeaderTokenizer.Token.EOF)
                    break;

                if (token.getType() != ';')
                    throw new IOException("MIME Content-Type parameter is not introduced with a semicolon: " + token.getValue());

                // Get name
                token = ht.next();
                if (token.getType() == HeaderTokenizer.Token.EOF)
                    throw new IOException("MIME Content-Type parameter name missing");

                if (token.getType() != HeaderTokenizer.Token.ATOM)
                    throw new IOException("MIME Content-Type parameter name is not an atom: " + token.getValue());

                String name = token.getValue();
                if (params.containsKey(name))
                    throw new IOException("MIME Content-Type parameter name occurs more than once: " + token.getValue());

                // eat =
                token = ht.next();
                if (token.getType() != '=')
                    throw new IOException("MIME Content-Type parameter name is not followed by an equals: " + token.getValue());

                // Get value
                token = ht.next();
                if (token.getType() == HeaderTokenizer.Token.EOF)
                    throw new IOException("MIME Content-Type parameter value missing");

                if (token.getType() != HeaderTokenizer.Token.ATOM && token.getType() != HeaderTokenizer.Token.QUOTEDSTRING)
                    throw new IOException("MIME Content-Type parameter value is not an atom or quoted string: " + token.getValue());

                String value = token.getValue();
                params.put(name, value);
            }

            return new ContentTypeHeader(type, subtype, params);
        } catch (ParseException e) {
            throw new CausedIOException("Unable to parse MIME header", e);
        }
    }

    /**
     * Convert MIME charset into Java encoding.
     *
     * @return the name of the Java encoding corresponding to the charset of this content-type header,
     *         or UTF-8 if there isn't any.  Always returns some string, never null.  The returned encoding
     *         is not guaranteed to be meaningful on this system, however.
     */
    public String getEncoding() {
        if (javaEncoding == null) {
            this.mimeCharset = getParam("charset");

            if (mimeCharset == null) {
                logger.finest("No charset value found in Content-Type header; assuming " + ENCODING);
                javaEncoding = ENCODING;
            } else {
                String tmp = MimeUtility.javaCharset(mimeCharset);
                if ("UTF8".equalsIgnoreCase(tmp))
                    javaEncoding = ENCODING;
                else
                    javaEncoding = tmp;
            }
        }
        return javaEncoding;
    }

    /** @return the type, ie "text".  never null */
    public String getType() {
        return type;
    }

    /** @return the subtype, ie "xml". never null */
    public String getSubtype() {
        return subtype;
    }

    /**
     * Get the multipart boundary, if this content type isMultipart.
     *
     * @return the Multipart boundary.  Never null or empty.
     * @throws IllegalStateException if not isMultipart().
     */
    public String getMultipartBoundary() {
        if (!isMultipart())
            throw new IllegalStateException("Content-Type is not multipart");
        return getParam(MimeUtil.MULTIPART_BOUNDARY);
    }

    /** @return true if the type is "text" */
    public boolean isText() {
        return "text".equalsIgnoreCase(getType());
    }

    public boolean isApplication() {
        return "application".equalsIgnoreCase(getType());
    }

    /** @return true if the type is "multipart" */
    public boolean isMultipart() {
        return "multipart".equalsIgnoreCase(getType());
    }

    /** @return true if the type is "text/xml", "application/xml" or "application/<em>anything</em>+xml*/
    public boolean isXml() {
        if (isText() && "xml".equalsIgnoreCase(getSubtype())) return true;
        if (isApplication()) {
            if ("xml".equalsIgnoreCase(getSubtype())) {
                return true;
            } else {
                int ppos = getSubtype().indexOf("+");
                if (ppos >= 0) {
                    if ("xml".equalsIgnoreCase(getSubtype().substring(ppos+1))) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Check if this content type header matches the specified pattern.
     *
     * @param type     type to match, or null or "*" to match any type
     * @param subtype  subtype to match, or null or "*" to match any type
     * @return true iff. this content type matched the specified pattern
     */
    public boolean matches(String type, String subtype) {
        if (type != null && !type.equals("*"))
            if (!getType().equals(type))
                return false;
        if (subtype != null && !subtype.equals("*")) {
            // If they wan't text/enriched, we'll consider ourselves to match if we are text/plain
            if ("text".equals(type) && "enriched".equals(subtype) && "plain".equals(getSubtype()))
                return true;
            if (!getSubtype().equals(subtype))
                return false;
        }
        return true;
    }

    protected void writeParam(OutputStream os, String name, String value) throws IOException {
        if (CHARSET.equalsIgnoreCase(name)) {
            getEncoding();
            os.write(name.getBytes(ENCODING));
            os.write('=');

            // Convert Java encoding into MIME charset for the payload
            String charsetValue = mimeCharset;
            if (charsetValue == null && javaEncoding != null)
                    charsetValue = MimeUtility.mimeCharset(javaEncoding);

            if (charsetValue == null)
                charsetValue = DEFAULT_CHARSET_MIME;

            os.write(MimeUtility.quote(charsetValue.toLowerCase(), HeaderTokenizer.MIME).getBytes(ENCODING));
        } else {
            super.writeParam(os, name, value);
        }
    }
}
