/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.mime;

import com.l7tech.common.http.HttpHeader;
import com.l7tech.common.io.BufferPoolByteArrayOutputStream;

import javax.mail.internet.HeaderTokenizer;
import javax.mail.internet.MimeUtility;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

/**
 * Encapsulates a MIME header and main value, and optional extra parameters after the main value.
 * Example:
 *    <code>Content-Type: text/html; charset=UTF-8</code>
 * would be encoded as
 *    name="Content-Type"
 *    mainValue="text/html"
 *    params={charset=>"UTF-8"}
 */
public class MimeHeader implements HttpHeader {
    /** Encoding used by MIME headers.  Actually limited to 7-bit ASCII per RFC, but UTF-8 is a safer choice. */
    public static final String ENCODING = "UTF-8";

    // Bytes of common stuff needed when serializing mime header
    static final byte[] CRLF;
    protected static final byte[] SEMICOLON;
    protected static final byte[] COLON;

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
    private final String mainValue;
    protected final Map params;

    protected byte[] serializedBytes;
    private String fullValue = null;

    /**
     * Create a new MimeHeader with the specified name, main value, and parameters.
     *
     * @param name   the name of the header, preferably in canonical form, not including the colon, ie "Content-Type".
     *               must not be empty or null.
     * @param value  the mainValue, ie "text/xml", not including any params that have already been parsed out into params.
     *               May be empty, but must not be null.
     * @param params the parameters, ie {charset=>"utf-8"}.  May be empty or null.
     *               Caller must not modify this map after giving it to this constructor.
     *               Caller is responsible for ensuring that, if a map is provided, lookups in the map are case-insensitive.
     *
     */
    protected MimeHeader(String name, String value, Map params) {
        if (name == null || value == null)
            throw new IllegalArgumentException("name and value must not be null");
        if (name.length() < 1)
            throw new IllegalArgumentException("name must not be empty");
        this.name = name;
        this.mainValue = value;
        this.params = params == null ? Collections.EMPTY_MAP : Collections.unmodifiableMap(params);
    }

    /**
     * Create a MIME header with the specified name, using the specified full value (including parameters, if any).
     * If name is Content-Type, a ContentTypeHeader instance will be returned.  In any case the returned
     * object will be a MimeHeader.
     *
     * @param name the name, ie "Content-Type".  may not be null
     * @param value the full value, ie "text/xml; charset=utf-8".  may not be null
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

    /**
     * Get the mainValue of this header, possibly not including parameters.
     * <p>
     * If this header was not one whose format was recognized (ie, was not Content-Type),
     * any parameters will have been left unparsed and will be returned as part of getValue().
     * <p>
     * For headers with a predefined value format (ie, "Content-Type: text/xml; charset=utf-8"),
     * this will return only the main value (ie, "text/xml")
     *
     * @return The mainValue of this header, possibly not including parameters.  Never null.
     */
    public String getMainValue() {
        return mainValue;
    }

    /** @return the specified parameter, or null if it didn't exist. */
    public String getParam(String name) {
        return (String)params.get(name);
    }

    /** @return the entire params map.  Never null. */
    protected Map getParams() {
        return params;
    }

    /** @return the ENTIRE header string, including name and trailing CRLF, ie "Content-Type: text/xml; charset=utf-8\r\n" */
    public String toString() {
        BufferPoolByteArrayOutputStream baos = new BufferPoolByteArrayOutputStream();
        try {
            write(baos);
            return baos.toString(ENCODING);
        } catch (IOException e) {
            throw new RuntimeException(e); // can't happen, it's a baos
        } finally {
            baos.close();
        }
    }

    public String getFullValue() {
        if (fullValue != null)
            return fullValue;
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream(32);
            writeFullValue(out);
            return fullValue = out.toString(ENCODING);
        } catch (IOException e) {
            throw new RuntimeException(e); // can't happen
        }
    }

    /**
     * Get the serialized byte form of this MimeHeader.
     *
     * @return the byte array representing the serialized form.  Never null or zero-length.
     */
    byte[] getSerializedBytes() {
        if (serializedBytes == null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(32);
            try {
                write(baos);
            } catch (IOException e) {
                throw new RuntimeException(e); // can't happen
            }
            serializedBytes = baos.toByteArray();
        }
        return serializedBytes;
    }

    /** @return the length of the serialized form of this MimeHeader. */
    int getSerializedLength() {
        return getSerializedBytes().length;
    }

    /** Reserialize entire header including name and trailing CRLF. */
    void write(OutputStream os) throws IOException {
        if (serializedBytes != null) {
            os.write(serializedBytes);
            return;
        }

        os.write(getName().getBytes(ENCODING));
        os.write(COLON);
        writeFullValue(os);
        os.write(CRLF);
    }

    /** Write just the complete value part of this header, including all params. */
    void writeFullValue(OutputStream os) throws IOException {
        os.write(getMainValue().getBytes(ENCODING));
        for (Iterator i = params.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry)i.next();
            String name = (String)entry.getKey();
            String value = (String)entry.getValue();
            os.write(SEMICOLON);
            os.write(' ');
            writeParam(os, name, value);
        }
    }

    protected void writeParam(OutputStream os, String name, String value) throws IOException {
        os.write(name.getBytes(ENCODING));
        os.write('=');
        os.write(MimeUtility.quote(value, HeaderTokenizer.MIME).getBytes(ENCODING));
    }
}
