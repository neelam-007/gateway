package com.l7tech.common.mime;

import com.l7tech.common.http.HttpHeader;
import com.l7tech.util.Charsets;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.PoolByteArrayOutputStream;
import com.l7tech.util.SyspropUtil;

import javax.mail.internet.HeaderTokenizer;
import javax.mail.internet.MimeUtility;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
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
    private static final Logger logger = Logger.getLogger( MimeHeader.class.getName() );

    public static final String PROP_PRESERVE_FORMAT = "com.l7tech.common.mime.preserveFormat";
    public static final boolean PRESERVE_FORMAT = ConfigFactory.getBooleanProperty( PROP_PRESERVE_FORMAT, true );

    // Common byte strings needed when serializing mime headers
    static final byte[] CRLF = "\r\n".getBytes( MimeUtil.ENCODING );
    protected static final byte[] SEMICOLON = "; ".getBytes( MimeUtil.ENCODING) ;
    protected static final byte[] COLON = ": ".getBytes( MimeUtil.ENCODING );

    private final String fullValue;
    private final String name;
    private final String mainValue;
    private final boolean hasParams;

    /** Holds a ref to null if a complete parse/validate has not yet been done for this mime header; otherwise holds MIME params or an empty map if there are none. */
    protected final AtomicReference<Map<String, String>> params = new AtomicReference<Map<String, String>>();

    protected byte[] serializedBytes;

    /**
     * Create a new MimeHeader with lazily-parsed parameters.
     *
     * @param name   the name of the header, preferably in canonical form, not including the colon, ie "Content-Type".
     *               must not be empty or null.
     * @param mainValue  the mainValue, ie "text/xml", not including any params that may be present in the fullValue.
     * @param fullValue The full value for the header, with original formatting, or null if the same as mainValue (in which case hasParams must be null).
     * @param hasParams true if the fullValue may contain parameters that may need to be parsed out of it.  False if the full
     *                  value is known not to contain parameters.
     */
    protected MimeHeader(final String name, final String mainValue, final String fullValue, final boolean hasParams) {
        if (name == null || mainValue == null)
            throw new IllegalArgumentException("name, mainValue, and fullValue must be provided");
        if (name.length() < 1)
            throw new IllegalArgumentException("name must not be empty");
        if (fullValue == null && hasParams)
            throw new IllegalArgumentException("fullValue must be provided if hasParams is true");
        this.name = name;
        this.mainValue = mainValue;
        this.params.set(null);
        this.fullValue = fullValue != null ? fullValue : mainValue;
        this.hasParams = hasParams;
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
     * @param header The full value for the header, with original formatting.  Required.
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
        this.params.set(params == null ? Collections.<String, String>emptyMap() : Collections.unmodifiableMap(params));
        this.fullValue = header;
        this.hasParams = params != null && !params.isEmpty();
    }

    /**
     * Create a MIME header with the specified name, using the specified full value (including parameters, if any).
     * If name is Content-Type, a ContentTypeHeader instance will be returned.  In any case the returned
     * object will be a MimeHeader.
     *
     * @param name the name, ie "Content-Type".  may not be null
     * @param fullValue the full value, ie "text/xml; charset=utf-8".  may not be null
     * @return the parsed MimeHeader
     * @throws IOException if the value is not a valid MIME value.
     */
    public static MimeHeader parseValue(String name, String fullValue) throws IOException {
        String value = fullValue;
        if (MimeUtil.CONTENT_TYPE.equalsIgnoreCase(name))
            return ContentTypeHeader.parseValue(fullValue);
        if (MimeUtil.CONTENT_LENGTH.equalsIgnoreCase(name)) {
            try {
                value = String.valueOf(parseNumericValue(fullValue));
            } catch (NumberFormatException e) {
                throw new IOException("Invalid MIME Content-Length header value: " + value);
            }
        }
        return new MimeHeader(name, value, fullValue, true);
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
     * @return true if this header has parameters available via {@link #getParams()}.
     */
    public boolean hasParams() {
        return hasParams;
    }

    /**
     * @param name the name of the parameter to get.
     * @return the specified parameter, or null if it didn't exist.
     */
    public String getParam(String name) {
        return getParams().get(name);
    }

    /**
     * Read the parameter map for this MIME header.
     * This will trigger a full parse/validate of this MIME header value, if one has not already been done.
     *
     * @return the entire params map.  Never null but may be empty.
     */
    public Map<String, String> getParams() {
        Map<String, String> ret = params.get();
        if (ret == null) {
            try {
                ret = parseParams();
                params.set(ret);
            } catch (IOException e) {
                logger.log(Level.FINE, "Unable to get params because header format is not valid", e);
                ret = Collections.emptyMap();
            }
        }
        return ret;
    }

    /** @return the ENTIRE header string, including name and trailing CRLF, ie "Content-Type: text/xml; charset=utf-8\r\n" */
    @Override
    public String toString() {
        PoolByteArrayOutputStream baos = new PoolByteArrayOutputStream();
        try {
            write(baos);
            return baos.toString( MimeUtil.ENCODING );
        } catch (IOException e) {
            throw new RuntimeException(e); // can't happen, it's a baos
        } finally {
            baos.close();
        }
    }

    @Override
    public String getFullValue() {
        return fullValue;
    }

    /**
     * Get the serialized byte form of this MimeHeader.
     *
     * @return the byte array representing the serialized form.  Never null or zero-length.
     */
    byte[] getSerializedBytes() {
        if (serializedBytes == null) {
            PoolByteArrayOutputStream baos = new PoolByteArrayOutputStream(32);
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

        os.write(getName().getBytes( MimeUtil.ENCODING ));
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
        if ( PRESERVE_FORMAT ) {
            os.write( fullValue.getBytes( MimeUtil.ENCODING ) );
        } else {
            os.write(getMainValue().getBytes( MimeUtil.ENCODING ));
            for (Map.Entry<String, String> entry : getParams().entrySet()) {
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
        os.write(name.getBytes( MimeUtil.ENCODING ));
        os.write('=');
        os.write(MimeUtility.quote(value, HeaderTokenizer.MIME).getBytes( MimeUtil.ENCODING ));
    }

    /**
     * Parse the parameters and validate the header format.
     * Subclasses can override this to handle their expected parameter format.
     * <p/>
     * This method always returns an empty map.
     *
     * @return the parameter map.  Never null but may be empty.
     * @throws IOException if the parameters cannot be parsed/validated.
     */
    protected Map<String, String> parseParams() throws IOException {
        return Collections.emptyMap();
    }
}
