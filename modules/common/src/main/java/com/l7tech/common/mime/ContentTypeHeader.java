/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.mime;

import com.l7tech.util.CausedIOException;
import com.l7tech.util.SyspropUtil;

import javax.mail.internet.HeaderTokenizer;
import javax.mail.internet.MimeUtility;
import javax.mail.internet.ParseException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a MIME Content-Type header.
 */
public class ContentTypeHeader extends MimeHeader {
    private static final Logger logger = Logger.getLogger(ContentTypeHeader.class.getName());

    public static final String PROP_STRICT_CHARSET = "com.l7tech.common.mime.strictCharset";
    public static final boolean STRICT_CHARSET = SyspropUtil.getBoolean(PROP_STRICT_CHARSET, false);

    public static final ContentTypeHeader OCTET_STREAM_DEFAULT; // application/octet-stream
    public static final ContentTypeHeader TEXT_DEFAULT; // text/plain; charset=UTF-8
    public static final ContentTypeHeader XML_DEFAULT; // text/xml; charset=UTF-8
    public static final ContentTypeHeader SOAP_1_2_DEFAULT; // appliacation/soap+xml; charset=UTF-8
    public static final ContentTypeHeader APPLICATION_X_WWW_FORM_URLENCODED; // application/x-www-form-urlencoded
    public static final ContentTypeHeader APPLICATION_JSON; // appliacation//json; charset=UTF-8
    public static final String CHARSET = "charset";
    public static final String DEFAULT_CHARSET_MIME = "utf-8";
    public static final Charset DEFAULT_HTTP_ENCODING = Charset.forName("ISO8859-1"); // See RFC2616 s3.7.1

    /**
     * AtomicReference to the list of configured textual content types. Usages do not need to synchronize.
     * Once the list is obtained via get(), it is safe to use as it will be copied if written to.
     * Never give this atomic reference a null reference.
     */
    private static AtomicReference<CopyOnWriteArrayList<ContentTypeHeader>> refToContentTypes =
            new AtomicReference<CopyOnWriteArrayList<ContentTypeHeader>>(new CopyOnWriteArrayList<ContentTypeHeader>());

    static {
        try {
            OCTET_STREAM_DEFAULT = parseValue("application/octet-stream");
            OCTET_STREAM_DEFAULT.getEncoding();
            TEXT_DEFAULT = parseValue("text/plain; charset=UTF-8");
            XML_DEFAULT = parseValue("text/xml; charset=UTF-8");
            XML_DEFAULT.getEncoding();
            SOAP_1_2_DEFAULT = parseValue("application/soap+xml; charset=UTF-8");
            APPLICATION_X_WWW_FORM_URLENCODED = parseValue("application/x-www-form-urlencoded");
            APPLICATION_JSON = parseValue("application/json");
        } catch (Throwable e) {
            throw new Error(e);
        }
    }

    private final String type;
    private final String subtype;

    private Charset javaEncoding = null; // figured out lazy-like
    private String mimeCharset = null;

    /**
     * Create a new ContentTypeHeader with the specified type, subtype, and parameters.  Currently
     * if the type is "multipart", the subtype may only be "related".
     *
     * @param type   the major type, ie "text". may not be null
     * @param subtype the minor type, ie "xml". may not be null
     * @param params the parameters, ie {charset=>"utf-8"}.  must not be null.
     *               Caller must not modify this map after giving it to this constructor.
     *               Caller is responsible for ensuring that lookups in the map are case-insensitive.
     * @throws IllegalArgumentException if type is multipart, but boundary param is missing or empty; or,
     *                                  if type is multipart, but the subtype is other than "related"
     * @throws NullPointerException if type, subtype or param is null
     * @throws java.io.IOException if an attempt is made to create a multipart type with a missing or invalid boundary
     */
    private ContentTypeHeader(String type, String subtype, Map<String, String> params) throws IOException {
        super(MimeUtil.CONTENT_TYPE, type + "/" + subtype, params);
        this.type = type.toLowerCase();
        this.subtype = subtype.toLowerCase();

        if ("multipart".equalsIgnoreCase(type)) {
            String boundary = this.params.get("boundary");
            if (boundary == null || boundary.length() < 1)
                throw new IOException("Content-Type of type multipart must include a boundary parameter (RFC 2045 sec 5)");
            byte[] bytes = boundary.getBytes(ENCODING);
            for (byte aByte : bytes) {
                if (aByte < ' ' || aByte > 126)
                    throw new IOException("Content-Type multipart boundary contains illegal character; must be US-ASCII (RFC 2045)");
            }

        }
    }

    /**
     * Constructor for use when the value is known to be good.
     *
     * @param contentTypeHeaderValue The value for the header
     * @return The new ContentTypeHeader (never null)
     * @throws IllegalArgumentException if the contentTypeHeaderValue is not valid
     */
    public static ContentTypeHeader create( final String contentTypeHeaderValue ) {
        try {
            return parseValue( contentTypeHeaderValue );
        } catch ( IOException e ) {
            throw new IllegalArgumentException( e );
        }
    }

    /**
     * Parse a MIME Content-Type: header, not including the header name and colon.
     * Example: <code>parseValue("text/html; charset=\"UTF-8\"")</code>
     *
     * @param contentTypeHeaderValue the header value to parse
     * @return a ContentTypeHeader instance.  Never null.
     * @throws java.io.IOException  if the specified header value was missing, empty, or syntactically invalid
     */
    public static ContentTypeHeader parseValue(String contentTypeHeaderValue) throws IOException {
        if (contentTypeHeaderValue == null || contentTypeHeaderValue.length() < 1)
            throw new IOException("MIME Content-Type header missing or empty");

        if (contentTypeHeaderValue.endsWith(";")) {
            contentTypeHeaderValue = contentTypeHeaderValue.substring(0, contentTypeHeaderValue.length()-1);    
        }

        HeaderTokenizer ht = new HeaderTokenizer(contentTypeHeaderValue, HeaderTokenizer.MIME, true);
        HeaderTokenizer.Token token;
        try {
            // Get type
            token = ht.next();
            if (token.getType() == HeaderTokenizer.Token.EOF)
                throw new IOException("MIME Content-Type type missing");
            if (token.getType() != HeaderTokenizer.Token.ATOM)
                throw new IOException("MIME Content-Type supertype is not an atom: " + contentTypeHeaderValue);

            String type = token.getValue();

            // Eat slash
            token = ht.next();
            if (token.getType() != '/')
                throw new IOException("MIME Content-Type supertype is not followed by a slash: " + contentTypeHeaderValue);

            // Get subtype
            token = ht.next();
            if (token.getType() == HeaderTokenizer.Token.EOF)
                throw new IOException("MIME Content-Type subtype missing");
            if (token.getType() != HeaderTokenizer.Token.ATOM)
                throw new IOException("MIME Content-Type subtype is not an atom: " + contentTypeHeaderValue);
            String subtype = token.getValue();

            // Check for parameters
            Map<String, String> params = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
            for (;;) {
                token = ht.next();

                if (token.getType() == HeaderTokenizer.Token.EOF)
                    break;

                if (token.getType() != ';')
                    throw new IOException("MIME Content-Type parameter is not introduced with a semicolon: " + contentTypeHeaderValue);

                // Get name
                token = ht.next();
                if (token.getType() == HeaderTokenizer.Token.EOF)
                    throw new IOException("MIME Content-Type parameter name missing");

                if (token.getType() != HeaderTokenizer.Token.ATOM)
                    throw new IOException("MIME Content-Type parameter name is not an atom: " + contentTypeHeaderValue);

                String name = token.getValue();
                if (params.containsKey(name))
                    throw new IOException("MIME Content-Type parameter name occurs more than once: " + contentTypeHeaderValue);

                // eat =
                token = ht.next();
                if (token.getType() != '=')
                    throw new IOException("MIME Content-Type parameter name is not followed by an equals: " + contentTypeHeaderValue);

                // Get value
                StringBuffer value = new StringBuffer();
                boolean sawQuotedString = false;
                for (;;) {
                    token = ht.peek();
                    int tokenType = token.getType();
                    if (tokenType == HeaderTokenizer.Token.EOF || tokenType == ';')
                        break;

                    token = ht.next();

                    if (tokenType == HeaderTokenizer.Token.QUOTEDSTRING) {
                        if (sawQuotedString)
                            throw new IOException("MIME Content-Type parameter value has more than one quoted string: " + contentTypeHeaderValue);
                        sawQuotedString = true;
                        value.append(token.getValue());
                        continue;
                    }

                    if (tokenType == HeaderTokenizer.Token.ATOM) {
                        value.append(token.getValue());
                        continue;
                    }

                    if (tokenType > 0 && !Character.isISOControl(tokenType)) {
                        value.append((char)tokenType);
                        continue;
                    }

                    throw new IOException("MIME Content-Type parameter value had unexpected token: " + token.getType() + " in: " + contentTypeHeaderValue);
                }

                params.put(name, value.toString());
            }

            return new ContentTypeHeader(type, subtype, params);
        } catch (ParseException e) {
            throw new CausedIOException("Unable to parse MIME header", e);
        }
    }

    /**
     * Convert MIME charset into Java encoding.
     *
     * @return the Java Charset corresponding to the charset of this content-type header,
     *         or {@link #DEFAULT_HTTP_ENCODING} if there isn't any.  Always returns some Charset, never null.
     *         The returned Charset may be a default value if the content header did not specify one,
     *         or if the one it specified is not recognized by this system.
     */
    public Charset getEncoding() {
        if (javaEncoding == null) {
            this.mimeCharset = getParam("charset");

            if (mimeCharset == null) {
                if (logger.isLoggable(Level.FINEST)) logger.finest("No charset value found in Content-Type header; using " + DEFAULT_HTTP_ENCODING);
                javaEncoding = DEFAULT_HTTP_ENCODING;
            } else {
                String tmp = MimeUtility.javaCharset(mimeCharset);
                if ("UTF8".equalsIgnoreCase(tmp)) {
                    javaEncoding = ENCODING;
                } else {
                    try {
                        javaEncoding = Charset.forName(tmp);
                    } catch (UnsupportedCharsetException e) {
                        if (STRICT_CHARSET) throw e;
                        if (logger.isLoggable(Level.FINEST)) logger.finest("Unrecognized charset in Content-Type header; using " + DEFAULT_HTTP_ENCODING);
                        javaEncoding = DEFAULT_HTTP_ENCODING;
                    }
                }
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

    /**
     * See if this content type can be represented as text. This only checks the type and possibly the subtype of
     * this content type. It is still possible that the first part of the mime knob is not text.
     * <p/>
     * The build in textual types are text, XML, JSON and Application Form URL encoded data.
     * Other types are configurable via static method {@link com.l7tech.common.mime.ContentTypeHeader#setConfigurableTextualContentTypes(ContentTypeHeader...)}
     *
     * @return true if content-type is textual, false otherwise. Decision is only based on the content type.
     */
    public boolean isTextualContentType() {

        if (isText() || isXml() || isJson() || isApplicationFormUrlEncoded()) return true;

        final CopyOnWriteArrayList<ContentTypeHeader> textualContentTypes = refToContentTypes.get();
        for (ContentTypeHeader otherType : textualContentTypes) {
            if (otherType.getType().equalsIgnoreCase(this.getType()) &&
                    otherType.getSubtype().equalsIgnoreCase(this.getSubtype()))
                return true;
        }

        return false;
    }

    /**
     * Set the list of configured textual content types.
     *  
     * @param typeHeaders Array of ContentTypeHeader's which represent textual data. Pass in null to clear the list.
     */
    public static void setConfigurableTextualContentTypes(ContentTypeHeader ... typeHeaders) {
        if (typeHeaders == null || typeHeaders.length == 0) {
            refToContentTypes.set(new CopyOnWriteArrayList<ContentTypeHeader>());
        } else {
            refToContentTypes.set(new CopyOnWriteArrayList<ContentTypeHeader>(typeHeaders));
        }
    }

    /** @return true if the type is "text" */
    public boolean isText() {
        return "text".equalsIgnoreCase(getType());
    }

    public boolean isApplication() {
        return "application".equalsIgnoreCase(getType());
    }

    public boolean isApplicationFormUrlEncoded(){
        return isApplication() && "x-www-form-urlencoded".equalsIgnoreCase(getSubtype());    
    }

    public boolean isJson() {
        return "application".equalsIgnoreCase(getType()) && "json".equalsIgnoreCase(getSubtype());
    }

    /** @return true if the type is "multipart" */
    public boolean isMultipart() {
        return "multipart".equalsIgnoreCase(getType());
    }

    /**
     * Check if this type is for some kind of textual XML.
     *
     * Note that this can be true for xhtml content.
     *
     * @return true if the type is "text/xml", "application/xml" or "application/<em>anything</em>+xml
     */
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
     * Check if this type is for some kind of textual (X)HTML.
     *
     * <ul>
     * <li>text/html</li>
     * <li>application/xhtml+xml</li>
     * </ul>
     *
     * Note that it is possible for isHtml and isXml to both be true.
     *
     * @return true if textual html content.
     */
    public boolean isHtml() {
        return (isText() && "html".equalsIgnoreCase(getSubtype()))
             ||(isApplication() && "xhtml+xml".equalsIgnoreCase(getSubtype()));
    }

    /**
     * Check if this type is for SOAP.
     *
     * <ul>
     * <li>application/soap+xml</li>
     * </ul>
     *
     * Note that it is possible for isHtml and isXml to both be true.
     *
     * @return true if textual html content.
     */
    public boolean isSoap12() {
        return isApplication() && "soap+xml".equalsIgnoreCase(getSubtype());
    }
    
    /**
     * Check if this content type header matches the type and subtype of the given
     * content type header.
     *
     * @param contentTypeHeader header with the type and subtype to match
     * @return true iff. this content type matched the specified pattern
     * @see com.l7tech.common.mime.ContentTypeHeader#matches(String,String) matches
     */
    public boolean matches(ContentTypeHeader contentTypeHeader) {
        return matches(contentTypeHeader.getType(), contentTypeHeader.getSubtype());
    }

    /**
     * Check if this content type header matches the specified pattern.
     *
     * @param type     type to match, or null or "*" to match any type
     * @param subtype  subtype to match, or null or "*" to match any type
     * @return true iff. this content type matched the specified pattern
     */
    public boolean matches(String type, String subtype) {
        type = type == null ? null : type.toLowerCase();
        subtype = subtype == null ? null : subtype.toLowerCase();
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

    /**
     *
     */
    public String toString() {
        return "ContentTypeHeader()[type='"+type+"'; subtype='"+subtype+"']";
    }

    /**
     *
     */
    protected void writeParam(OutputStream os, String name, String value) throws IOException {
        if (CHARSET.equalsIgnoreCase(name)) {
            getEncoding();
            os.write(name.getBytes(ENCODING));
            os.write('=');

            // Convert Java encoding into MIME charset for the payload
            String charsetValue = mimeCharset;
            if (charsetValue == null && javaEncoding != null)
                    charsetValue = MimeUtility.mimeCharset(javaEncoding.name());

            if (charsetValue == null)
                charsetValue = DEFAULT_CHARSET_MIME;

            os.write(MimeUtility.quote(charsetValue.toLowerCase(), HeaderTokenizer.MIME).getBytes(ENCODING));
        } else {
            super.writeParam(os, name, value);
        }
    }

    @SuppressWarnings({"RedundantIfStatement"})
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ContentTypeHeader)) return false;

        ContentTypeHeader that = (ContentTypeHeader) o;

        if (subtype != null ? !subtype.equals(that.subtype) : that.subtype != null) return false;
        if (type != null ? !type.equals(that.type) : that.type != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (subtype != null ? subtype.hashCode() : 0);
        return result;
    }
}
