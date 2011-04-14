/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.mime;

import com.l7tech.common.http.HttpHeader;
import com.l7tech.util.PoolByteArrayOutputStream;
import com.l7tech.util.Charsets;

import javax.mail.internet.HeaderTokenizer;
import javax.mail.internet.MimeUtility;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Pattern;

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
    public static final Charset ENCODING = Charsets.UTF8;

    // Common byte strings needed when serializing mime headers
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
    protected final Map<String, String> params;

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
    protected MimeHeader(String name, String value, Map<String, String> params) {
        this( name, value, params, null );
    }

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
     * @param header The full value for the header
     *
     */
    protected MimeHeader( final String name,
                          final String value,
                          final Map<String, String> params,
                          final String header ) {
        if (name == null || value == null)
            throw new IllegalArgumentException("name and value must not be null");
        if (name.length() < 1)
            throw new IllegalArgumentException("name must not be empty");
        this.name = name;
        this.mainValue = value;
        this.params = params == null ? Collections.<String, String>emptyMap() : Collections.unmodifiableMap(params);
        this.fullValue = header;
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
                value = String.valueOf(parseNumericValue(value));
            } catch (NumberFormatException e) {
                throw new IOException("Invalid MIME Content-Length header value: " + value);
            }
        }
        return new MimeHeader(name, value, null);
    }

    private static final Pattern COMMAPAT = Pattern.compile("\\s*,\\s*");

    /**
     * Attempt to parse an HTTP header value as a number, permitting a multi-valued header (such as "23, 23, 23")
     * as long as all values are the same number.
     *
     * @param possiblyMultivaluedValue the value to parse.  Required.
     * @return the numeric value of the header value.
     * @throws NullPointerException if the value is null.
     * @throws NumberFormatException if the value is empty or non-numeric, or if there are multiple values that don't all agree.
     */
    public static long parseNumericValue(String possiblyMultivaluedValue) {
        if (possiblyMultivaluedValue == null)
            throw new NullPointerException();
        try {
            return Long.parseLong(possiblyMultivaluedValue);
        } catch (NumberFormatException nfe) {
            // See if it has multiple identical values (Bug #7353)
            String[] vals = COMMAPAT.split(possiblyMultivaluedValue.trim());
            if (vals.length < 1)
                throw nfe;
            long val = Long.parseLong(vals[0]);
            if (vals.length > 1) {
                for (int i = 1; i < vals.length; ++i) {
                    if (val != Long.parseLong(vals[i]))
                        throw new NumberFormatException("Multiple disagreeing header values: " + val + " / " + vals[i]);
                }
            }
            return val;
        }
    }

    @Override
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

    /**
     * @param name the name of the parameter to get.
     * @return the specified parameter, or null if it didn't exist.
     */
    public String getParam(String name) {
        return params.get(name);
    }

    /** @return the entire params map.  Never null. */
    public Map<String, String> getParams() {
        return params;
    }

    /** @return the ENTIRE header string, including name and trailing CRLF, ie "Content-Type: text/xml; charset=utf-8\r\n" */
    @Override
    public String toString() {
        PoolByteArrayOutputStream baos = new PoolByteArrayOutputStream();
        try {
            write(baos);
            return baos.toString(ENCODING);
        } catch (IOException e) {
            throw new RuntimeException(e); // can't happen, it's a baos
        } finally {
            baos.close();
        }
    }

    @Override
    public String getFullValue() {
        if (fullValue != null)
            return fullValue;
        PoolByteArrayOutputStream out = new PoolByteArrayOutputStream(32);
        try {
            writeFullValue(out);
            return fullValue = out.toString(ENCODING);
        } catch (IOException e) {
            throw new RuntimeException(e); // can't happen
        } finally {
            out.close();
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

    /**
     * Reserialize entire header including name and trailing CRLF.
     *
     * @param os the stream to which the header should be written.  Required.
     * @throws java.io.IOException if there is an IOException while writing to the stream
     */
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

    /**
     * Write just the complete value part of this header, including all params.
     *
     * @param os the stream to which the value part should be written.  Required.
     * @throws java.io.IOException if there is an IOException while writing to the stream
     */
    void writeFullValue(OutputStream os) throws IOException {
        if ( fullValue != null ) {
            os.write( fullValue.getBytes(ENCODING) );
        } else {
            os.write(getMainValue().getBytes(ENCODING));
            for (Map.Entry<String, String> entry : params.entrySet()) {
                String name = entry.getKey();
                String value = entry.getValue();
                os.write(SEMICOLON);
                writeParam(os, name, value);
            }
        }
    }

    /**
     * Write a single parameter name and value to the specified output stream.
     * This write the parameter name, and equals sign, and the parameter value.
     * The value is quoted per the MIME header rules.
     *
     * @param os the stream to which the parameter should be written.  Required.
     * @param name parameter name.  Required.
     * @param value the raw (not yet quoted) parameter value.  Required.
     * @throws java.io.IOException if there is an IOException while writing to the stream
     */
    protected void writeParam(OutputStream os, String name, String value) throws IOException {
        os.write(name.getBytes(ENCODING));
        os.write('=');
        os.write(MimeUtility.quote(value, HeaderTokenizer.MIME).getBytes(ENCODING));
    }
}
