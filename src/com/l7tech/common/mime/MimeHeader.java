/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.mime;

import javax.mail.internet.HeaderTokenizer;
import javax.mail.internet.MimeUtility;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Encapsulates a MIME header and value, and optional extra parameters after the value.
 * Example:
 *    <code>Content-Type: text/html; charset=UTF-8</code>
 * would be encoded as
 *    name="Content-Type"
 *    value="text/html"
 *    params={charset=>"UTF-8"}
 */
public class MimeHeader {
    /** Encoding used by MIME headers.  Actually limited 7-bit ASCII, but UTF-8 is safer. */
    public static final String ENCODING = "UTF-8";

    // Bytes of common stuff needed when serializing mime header
    static final byte[] CRLF;
    private static final byte[] SEMICOLON;
    private static final byte[] COLON;

    static {
        try {
            CRLF = "\r\n".getBytes(ENCODING);
            SEMICOLON = "; ".getBytes(ENCODING);
            COLON = ": ".getBytes(ENCODING);
        } catch (Exception e) {
            throw new Error("Encoding not found: " + ENCODING);
        }
    }

    private final String name;
    private final String value;
    protected final Map params;

    protected MimeHeader(String name, String value, Map params) {
        if (name == null || value == null)
            throw new IllegalArgumentException("name and value must not be null");
        this.name = name;
        this.value = value;
        this.params = params == null ? new HashMap() : params;
    }

    /**
     * Create a MIME header with the specified name, using the specified value (including parameters, if any).
     * If name is Content-Type, a ContentTypeHeader instance will be returned.  In any case the returned
     * object will be a MimeHeader.
     *
     * @param name the name, ie "Content-Type".  may not be null
     * @param value the value, ie "text/xml; charset=utf8".  may not be null
     * @return the parsed MimeHeader
     * @throws IOException if the value is not a valid MIME value.
     */
    public static MimeHeader parseValue(String name, String value) throws IOException {
        if (MimeUtil.CONTENT_TYPE.equalsIgnoreCase(name))
            return ContentTypeHeader.parseValue(value);
        if (MimeUtil.CONTENT_LENGTH.equalsIgnoreCase(name)) {
            try {
                Long.parseLong(value);
            } catch (NumberFormatException e) {
                throw new IOException("Invalid MIME Content-Length header value: " + value);
            }
        }
        return new MimeHeader(name, value, null);
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    /** @return the specified parameter, or null if it didn't exist. */
    public String getParam(String name) {
        return (String)params.get(name);
    }

    public String toString() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            write(baos);
            return new String(baos.toByteArray(), ENCODING);
        } catch (IOException e) {
            throw new RuntimeException(e); // can't happen, it's a baos
        }
    }

    void write(OutputStream os) throws IOException {
        os.write(getName().getBytes(ENCODING));
        os.write(COLON);
        os.write(MimeUtility.quote(getValue(), HeaderTokenizer.MIME).getBytes(ENCODING));
        for (Iterator i = params.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry)i.next();
            String name = (String)entry.getKey();
            String value = (String)entry.getValue();
            os.write(SEMICOLON);
            os.write(name.getBytes(ENCODING));
            os.write('=');
            os.write(MimeUtility.quote(value, HeaderTokenizer.MIME).getBytes(ENCODING));
        }
        os.write(CRLF);
    }
}
