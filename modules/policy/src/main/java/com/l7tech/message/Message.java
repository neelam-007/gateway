package com.l7tech.message;

import com.l7tech.common.http.HttpConstants;
import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.EmptyInputStream;
import com.l7tech.common.mime.*;
import com.l7tech.util.CausedIllegalStateException;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.Functions;
import com.l7tech.xml.MessageNotSoapException;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents an abstract Message in the system.  This can be a request or a reply; over HTTP or JMS or transport
 * not yet determined; using SOAP, MIME, XML, or not yet set.  Any message at all.
 * <p>
 * All Messages and MessageFacets should be assumed <em>not</em> to be threadsafe.
 */
public final class Message implements Closeable {
    public static final String PROPERTY_ENABLE_ORIGINAL_DOCUMENT = "com.l7tech.message.enableOriginalDocument";

    private static boolean defaultEnableOriginalDocument = ConfigFactory.getBooleanProperty( PROPERTY_ENABLE_ORIGINAL_DOCUMENT, false );

    /**
     * enable this to enable XmlKnob.getOriginalDocument().
     * This is off by default since only certain messages need this.
     */
    private boolean enableOriginalDocument = defaultEnableOriginalDocument;

    private MessageFacet rootFacet;

    private Map<MessageRole, Message> relatedMessages = new HashMap<MessageRole, Message>();

    // Quick lookup knob cache
    private HttpRequestKnob httpRequestKnob;
    private HttpServletRequestKnob httpServletRequestKnob;
    private HttpResponseKnob httpResponseKnob;
    private HttpServletResponseKnob httpServletResponseKnob;
    private TcpKnob tcpKnob;
    private XmlKnob xmlKnob;
    private SoapKnob soapKnob;
    private MimeKnob mimeKnob;
    private SecurityKnob securityKnob;
    private JsonKnob jsonKnob;
    private boolean initialized;

    /**
     * Returns the xml part max bytes value set in the io.xmlPartMaxBytes cluster property
     * @return the xml part max bytes value set in the io.xmlPartMaxBytes cluster property
     */
    static public long getMaxBytes(){
        return MimeBody.getFirstPartMaxBytes();
    }
    /**
     * Create a Message with no facets.
     */
    public Message() {
    }

    /**
     * Create a Message pre-initialized with a MIME facet attached to the specified InputStream.
     *
     * @param sm  the StashManager to use for stashing MIME parts temporarily.  Must not be null.
     * @param outerContentType  the content type of the body InputStream.  Must not be null.
     * @param body an InputStream positioned at the first byte of body content for this Message.
     *             This <b>must not include</b> any outer headers (HTTP or otherwise) that may have
     *             accompanied the body of this Message.  To attach the outer headers to the message,
     *             see {@link #attachKnob} and {@link com.l7tech.message.HttpHeadersKnob}.
     * @throws IOException if there is a problem reading the initial boundary from a multipart/related body, or
     *                     if the message is multipart/related but contains no initial boundary.
     */
    public Message(StashManager sm,
                   ContentTypeHeader outerContentType,
                   InputStream body)
            throws IOException
    {
        initialize(sm, outerContentType, body);
    }

    /**
     * Create a Message pre-initialized with a Document.
     *
     * @param doc the Document to use.  Must not be null.
     * @param maxBytes message byte size limit. default 0 = unlimited
     */
    public Message(Document doc, long maxBytes) {
        initialize(doc, maxBytes);
    }
    public Message(Document doc) {
        initialize(doc,0);
    }

    /**
     * @return true if this message has been initialized by an external invocation of one of the initialize
     *         methods.
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Initialize, or re-initialize, a Message with a MIME facet attached to the specified InputStream.
     * <p>
     * With the exception of {@link PreservableFacet}s (such as those implementing {@link HttpRequestKnob} and {@link HttpResponseKnob}), which will be preserved in new facets,
     * any previously existing facets of this Message will be lost and replaced with a single MIME facet.
     *
     * @param sm  the StashManager to use for stashing MIME parts temporarily.  Must not be null.
     * @param outerContentType  the content type of the body InputStream.  Must not be null.
     * @param body an InputStream positioned at the first byte of body content for this Message.
     * @param firstPartMaxBytes first part byte size limit. default 0 = unlimited
     * @throws IOException if there is a problem reading the initial boundary from a multipart/related body, or
     *                     if the message is multipart/related but contains no initial boundary.
     */

    public void initialize( final StashManager sm,
                            final ContentTypeHeader outerContentType,
                            final InputStream body,
                            final long firstPartMaxBytes)
            throws IOException
    {
        List<PreservableFacet> preservables = getPreservableFacets();
        if (rootFacet != null) rootFacet.close(); // This will close the reqKnob and respKnob as well, but they don't do anything when closed
        rootFacet = null; // null it first just in case MimeFacet c'tor throws
        invalidateCachedKnobs();
        rootFacet = new MimeFacet(this, sm, outerContentType, body,firstPartMaxBytes);
        for (PreservableFacet preservable : preservables) {
            rootFacet = preservable.reattach(this, rootFacet);
        }
        invalidateCachedKnobs();
        initialized = true;
    }


    /**
     * Initialize, or re-initialize, a Message with a MIME facet attached to the specified InputStream.
     * <p>
     * With the exception of {@link PreservableFacet}s (such as those implementing {@link HttpRequestKnob} and {@link HttpResponseKnob}), which will be preserved in new facets,
     * any previously existing facets of this Message will be lost and replaced with a single MIME facet.
     *
     * @param sm  the StashManager to use for stashing MIME parts temporarily.  Must not be null.
     * @param outerContentType  the content type of the body InputStream.  Must not be null.
     * @param body an InputStream positioned at the first byte of body content for this Message.
     * @throws IOException if there is a problem reading the initial boundary from a multipart/related body, or
     *                     if the message is multipart/related but contains no initial boundary.
     */
    public void initialize( final StashManager sm,
                            final ContentTypeHeader outerContentType,
                            final InputStream body )
            throws IOException
    {
        initialize(sm, outerContentType,body,0);
    }


    /**
     * Initialize, or re-initialize, a Message with a memory-based MIME facet and an XML facet initialized with
     * the specified Document.
     * <p>
     * With the exception of {@link PreservableFacet}s (such as those implementing {@link HttpRequestKnob} and {@link HttpResponseKnob}), which will be preserved in new facets,
     * any previously existing facets of this Message will be lost and replaced with a single MIME facet.
     *
     * @param body the Document to replace this Message's current content
     * @param maxBytes message byte size limit. default 0 = unlimited
     */
    public void initialize(Document body, long maxBytes)
    {
        initialize( body, ContentTypeHeader.XML_DEFAULT, maxBytes );
    }

    /**
     * Initialize, or re-initialize, a Message with a memory-based MIME facet and an XML facet initialized with
     * the specified Document.
     * <p>
     * With the exception of {@link PreservableFacet}s (such as {@link HttpRequestKnob} and {@link HttpResponseKnob}), which will be preserved in new facets,
     * any previously existing facets of this Message will be lost and replaced with a single MIME facet.
     *
     * @param body the Document to replace this Message's current content
     */
    public void initialize(Document body)
    {
        initialize( body, ContentTypeHeader.XML_DEFAULT,0);
    }


    /**
     * Initialize, or re-initialize, a Message with a memory-based MIME facet and an XML facet initialized with
     * the specified Document.
     * <p>
     * With the exception of {@link PreservableFacet}s (such as {@link HttpRequestKnob} and {@link HttpResponseKnob}), which will be preserved in new facets,
     * any previously existing facets of this Message will be lost and replaced with a single MIME facet.
     *
     * @param body the Document to replace this Message's current content
     * @param contentTypeHeader the XML content type to use
     * @param firstPartMaxBytes first part byte size limit. default 0 = unlimited
     */
    public void initialize(Document body, ContentTypeHeader contentTypeHeader, long firstPartMaxBytes)
    {
        try {
            List<PreservableFacet> preservables = getPreservableFacets();
            if (rootFacet != null) rootFacet.close(); // This will close the reqKnob and respKnob as well, but they don't do anything when closed
            rootFacet = null;
            rootFacet = new MimeFacet(this, new ByteArrayStashManager(), contentTypeHeader, new EmptyInputStream(),firstPartMaxBytes);
            rootFacet = new XmlFacet(this, rootFacet);
            invalidateCachedKnobs();
            for (PreservableFacet preservable : preservables) {
                rootFacet = preservable.reattach(this, rootFacet);
            }
            getXmlKnob().setDocument(body);
            initialized = true;
        } catch (IOException e) {
            throw new RuntimeException(e); // can't happen, it's a byte array input stream
        } catch (SAXException e) {
            throw new RuntimeException(e); // can't happen, the content type is set to xml
        }
    }

    /**
     * Initialize, or re-initialize, a Message with a memory-based MIME facet and an XML facet initialized with
     * the specified Document.
     * <p>
     * With the exception of {@link PreservableFacet}s (such as those implementing {@link HttpRequestKnob} and {@link HttpResponseKnob}), which will be preserved in new facets,
     * any previously existing facets of this Message will be lost and replaced with a single MIME facet.
     *
     * @param body the Document to replace this Message's current content
     * @param contentTypeHeader the XML content type to use
     */
    public void initialize(Document body, ContentTypeHeader contentTypeHeader)
    {
        initialize(body, contentTypeHeader, 0);
    }

    /**
     * Initialize, or re-initialize, a Message with a memory-based MIME facet containing the specified body
     * bytes.
     *
     * @param contentType  the MIME content type.  Required.
     * @param bodyBytes the body bytes.  May be empty but must not be null.
     * @param firstPartMaxBytes first part byte size limit. default 0 = unlimited
     * @throws IOException if contentType is multipart, but the body does not contain the boundary or contains no parts
     */
    public void initialize(ContentTypeHeader contentType, byte[] bodyBytes, long firstPartMaxBytes) throws IOException {
        try {
            HttpRequestKnob reqKnob = getKnob(HttpRequestKnob.class);
            HttpResponseKnob respKnob = getKnob(HttpResponseKnob.class);
            if (rootFacet != null) rootFacet.close(); // This will close the reqKnob and respKnob as well, but they don't do anything when closed
            rootFacet = null;
            rootFacet = new MimeFacet(this, new ByteArrayStashManager(), contentType, new ByteArrayInputStream(bodyBytes),firstPartMaxBytes);
            invalidateCachedKnobs();
            if (reqKnob != null) attachHttpRequestKnob(reqKnob);
            if (respKnob != null) attachHttpResponseKnob(respKnob);
            initialized = true;
        } catch (IOException e) {
            throw new RuntimeException(e); // can't happen, it's a byte array input stream
        }
    }

    /**
     * Initialize, or re-initialize, a Message with a memory-based MIME facet containing the specified body
     * bytes.
     *
     * @param contentType  the MIME content type.  Required.
     * @param bodyBytes the body bytes.  May be empty but must not be null.
     * @throws IOException if contentType is multipart, but the body does not contain the boundary or contains no parts
     */
    public void initialize(ContentTypeHeader contentType, byte[] bodyBytes) throws IOException {
        initialize(contentType,bodyBytes,0);
    }


    public void setEnableOriginalDocument() {
        this.enableOriginalDocument = true;
    }

    public boolean isEnableOriginalDocument() {
        return enableOriginalDocument;
    }

    /**
     * Get the knob for the MIME facet of this Message.
     * <p/>
     * If this Message has not been initialized yet, this will return a MimeKnob for a zero-length message of
     * type application/octet-stream.  After this point the message will behave roughly as though it had been initialized
     * with a zero-length octet-stream (succeeding when queried for a MimeKnob), but with the notable exception
     * that {@link #isInitialized()} will continue to return false until the message is initialized for real.
     *
     * @return the MimeKnob for this Message.  Never null.
     */
    public MimeKnob getMimeKnob() {
        if (this.mimeKnob != null)
            return this.mimeKnob;
        MimeKnob mimeKnob = getKnob(MimeKnob.class);
        if (mimeKnob == null) {
            try {
                boolean wasInitialized = initialized;
                initialize(ContentTypeHeader.OCTET_STREAM_DEFAULT, new byte[0]);
                initialized = wasInitialized; // isInitialized() should still return false for this message if it has never been given "real" contents
                mimeKnob = this.mimeKnob != null ? this.mimeKnob : getKnob(MimeKnob.class);
                assert mimeKnob != null;
            } catch (IOException e) {
                throw new RuntimeException(e); // can't happen
            }
        }
        return mimeKnob;
    }

    /**
     * Get the knob for the JSON facet of this Message, creating one if necessary and possible.
     * If no JSON facet is currently installed, one will be created if there is a MIME facet whose first part's
     * conent-type is application/json
     * @return
     */
    public JsonKnob getJsonKnob(){
        if(this.jsonKnob != null)
           return this.jsonKnob;

        JsonKnob jsonKnob = getKnob(JsonKnob.class);
        if(jsonKnob == null) {
            rootFacet = new JsonFacet(this, rootFacet);
            invalidateCachedKnobs();
            jsonKnob = getKnob(JsonKnob.class);
            if (jsonKnob == null) throw new IllegalStateException(); // can't happen, we just made one
        }
        return jsonKnob;
    }

    /**
     * Get the knob for the XML facet of this Message, creating one if necessary and possible.
     * If no XML facet is currently installed, one will be created if there is a MIME facet whose
     * first part's content type is text/xml.
     *
     * @return the XmlKnob for this Message.  Never null.
     * @throws SAXException if the first part's content type is not text/xml.
     */
    public XmlKnob getXmlKnob() throws SAXException {
        if (this.xmlKnob != null)
            return this.xmlKnob;
        XmlKnob xmlKnob = getKnob(XmlKnob.class);
        if (xmlKnob == null) {
            if (!initialized)
                throw new SAXException("Message first part is not text/xml (message not initialized)");
            try {
                rootFacet = new XmlFacet(this, rootFacet);
                invalidateCachedKnobs();
                xmlKnob = getKnob(XmlKnob.class);
                assert xmlKnob != null;
            } catch (IOException e) {
                throw new CausedIllegalStateException(e); // can't happen, no XML facet yet
            }
        }
        return xmlKnob;
    }

    /**
     * Get the security knob for this Message.  Always succeeds; if a security knob does not yet exist, a new
     * one will be created.
     *
     * @return the security knob for this Message.  Never null.
     */
    public SecurityKnob getSecurityKnob() {
        SecurityKnob secKnob = getKnob(SecurityKnob.class);
        if (secKnob == null) {
            rootFacet = new SecurityFacet(this, rootFacet);
            invalidateCachedKnobs();
            secKnob = getKnob(SecurityKnob.class);
            if (secKnob == null) throw new IllegalStateException();
        }
        return secKnob;
    }

    /**
     * Check if this message is declared as containing JSON.  Does not actually parse the JSON, if it's there.
     * No exceptions are thrown except IOException, and that only in a situation that would be fatal to the Message
     * anyway.
     * <p>
     * If this method returns true, an JsonKnob will be present on this Message.
     *
     * @return true if this message has a first part declared as application/json, which has some content;
     *         false if this message has no first part or its first part isn't declared as JSON or has a length of 0.
     * @throws IOException if thrown while access the first part of the mime knob
     */
    public boolean isJson() throws IOException {

        if (jsonKnob != null)
            return true;

        if (getKnob(JsonKnob.class) != null)
            return true;

        MimeKnob mimeKnob = getKnob(MimeKnob.class);
        if (mimeKnob == null)
            return false;
        if (!mimeKnob.getFirstPart().getContentType().isJson())
            return false;

        // It's declared as JSON, check that there is some content
        HttpRequestKnob knob = getKnob(HttpRequestKnob.class);
        if (knob != null) {
            int length = knob.getIntHeader(HttpConstants.HEADER_CONTENT_LENGTH);
            if (length == 0) {
                return false;
            }
        }

        getJsonKnob();

        return true;
    }

    /**
     * Check if this message is declared as containing XML.  Does not actually parse the XML, if it's there.
     * No exceptions are thrown except IOException, and that only in a situation that would be fatal to the Message
     * anyway.
     * <p>
     * If this method returns true, an XmlKnob will be present on this Message.
     *
     * @return true if this message has a first part declared as text/xml, which has some content;
     *         false if this message has no first part or its first part isn't declared as XML or has a length of 0.
     * @throws IOException if XML serialization is necessary, and it throws IOException (perhaps due to a lazy DOM)
     */
    public boolean isXml() throws IOException {
        if (xmlKnob != null)
            return true;
        if (getKnob(XmlKnob.class) != null)
            return true;
        MimeKnob mimeKnob = getKnob(MimeKnob.class);
        if (mimeKnob == null)
            return false;
        if (!mimeKnob.getFirstPart().getContentType().isXml())
            return false;

        // It's declared as XML, check that there is some content
        HttpRequestKnob knob = getKnob(HttpRequestKnob.class);
        if (knob != null) {
            int length = knob.getIntHeader(HttpConstants.HEADER_CONTENT_LENGTH);
            if (length == 0) {
                return false;
            }
        }

        // Create the XML knob while we are here (won't actually try to parse yet)
        try {
            getXmlKnob();
            return true;
        } catch (SAXException e) {
            throw new CausedIllegalStateException("First part not XML", e); // can't happen here
        }
    }

    /**
     * Get the knob for the SOAP facet of this Message, creating one if necessary and possible.
     * If no SOAP facet is currently installed, one will be created if an XML facet can be created
     * and the message appears to be SOAP.
     *
     * @return the SoapKnob for this Message.  Never null.
     * @throws SAXException if the first part's content type is not text/xml.
     * @throws IOException if XML serialization throws IOException, perhaps due to a lazy Document.
     * @throws MessageNotSoapException if there is an XML document but it doesn't look like a valid SOAP envelope
     * @throws IllegalStateException if the SOAP MIME part has already been destructively read.
     */
    public SoapKnob getSoapKnob() throws SAXException, IOException, MessageNotSoapException {
        SoapKnob soapKnob = getKnob(SoapKnob.class);
        if (soapKnob == null) {
            if (!isSoap())
                throw new MessageNotSoapException();
            soapKnob = getKnob(SoapKnob.class);
            if (soapKnob == null)
                throw new IllegalStateException("isSoap() is true but there's no SoapKnob");
        }
        return soapKnob;
    }

    /**
     * Check if this message appears to contain a SOAP envelope.  This will fail fast if the message isn't XML,
     * but may need to begin parsing the XML if it is.
     * <p>
     * If this method returns true, a SoapKnob will be present on this Message.
     * <p>
     * If Tarari hardware was used to examine the message to determine if it was soap, and the message turned out
     * to be well-formed XML, a TarariKnob will be present on this Message regardless of whether or not it
     * was SOAP.
     *
     * @return true if this mesage appears to contain SOAP.
     * @throws SAXException if the XML in the first part's InputStream is not well formed
     * @throws IOException if there is a problem reading XML from the first part's InputStream; or,
     *                     if XML serialization is necessary, and it throws IOException (perhaps due to a lazy DOM)
     * @throws IllegalStateException if the SOAP MIME part has already been destructively read.
     */
    public boolean isSoap() throws IOException, SAXException {
        return isSoap(false);
    }

    /**
     * Check if this message appears to contain a SOAP envelope.  This will fail fast if the message isn't XML,
     * but may need to begin parsing the XML if it is.
     * <p>
     * If this method returns true, a SoapKnob will be present on this Message.
     * <p>
     * If Tarari hardware was used to examine the message to determine if it was soap, and the message turned out
     * to be well-formed XML, a TarariKnob will be present on this Message regardless of whether or not it
     * was SOAP.
     *
     * @param preferDOM true to prefer a DOM parse
     * @return true if this mesage appears to contain SOAP.
     * @throws SAXException if the XML in the first part's InputStream is not well formed
     * @throws IOException if there is a problem reading XML from the first part's InputStream; or,
     *                     if XML serialization is necessary, and it throws IOException (perhaps due to a lazy DOM)
     * @throws IllegalStateException if the SOAP MIME part has already been destructively read.
     */
    public boolean isSoap(boolean preferDOM) throws IOException, SAXException {
        if (this.soapKnob != null)
            return true;
        HttpRequestKnob hrk = getKnob(HttpRequestKnob.class);
        if (hrk != null && hrk.getMethod() != HttpMethod.POST)
            return false;
        if (getKnob(SoapKnob.class) != null)
            return true;
        if (!isXml())
            return false;

        SoapInfo info = null;

        // See if we have already inspected a non-SOAP XML message
        TarariKnob tk = getKnob(TarariKnob.class);
        if (tk != null && !preferDOM)
            info = tk.getSoapInfo();

        if (info == null) {
            // We have an XML knob but no SOAP knob.  See if we can create a SOAP knob.
            try {
                if (preferDOM) {
                    getXmlKnob().getDocumentReadOnly();   
                }
                info = SoapFacet.getSoapInfo(this);
            } catch (NoSuchPartException e) {
                throw new SAXException(e);
            }
        }

        if (info == null || !info.isSoap())
            return false;

        rootFacet = new SoapFacet(this, rootFacet, info);
        invalidateCachedKnobs();
        return true;
    }

    /**
     * Check if this message is a Http request.
     * @return true if the message is a Http request
     */
    public boolean isHttpRequest() {
        return getKnob(HttpRequestKnob.class) != null;
    }

    /**
     * Check if this message is a Http response.
     * @return true if the message is a Http response
     */
    public boolean isHttpResponse() {
        return getKnob(HttpResponseKnob.class) != null;
    }

    /**
     * Configure this Message as an HTTP request.  This attaches sources of TCP and HTTP request transport metadata to this
     * Message.  A Message may have at most one HTTP request knob.  An HTTP request knob may cooexist with
     * an HTTP response knob.
     *
     * @param httpRequestKnob  the source of HTTP request transport metadata.  May not be null.
     * @throws IllegalStateException if this Message is already configured as an HTTP request
     */
    public void attachHttpRequestKnob(HttpRequestKnob httpRequestKnob) throws IllegalStateException {
        if (getKnob(HttpRequestKnob.class) != null)
            throw new IllegalStateException("This Message is already configured as an HTTP request");
        rootFacet = new HttpRequestFacet(this, rootFacet, httpRequestKnob);
        invalidateCachedKnobs();
    }

    /**
     * Configure this Message as a JMS Message.  This attaches a {@code javax.jms.Message} to this
     * Message.  A Message may have at most one JMS knob.
     *
     * @param jmsKnob source of JMS message.  May not be null.
     * @throws IllegalStateException if this Message is already configured as a JMS Message
     */
    public void attachJmsKnob(JmsKnob jmsKnob) throws IllegalStateException {
        if (getKnob(JmsKnob.class) != null)
            throw new IllegalStateException("This Message is already configured as a JMS Message");
        rootFacet = new JmsFacet(this, rootFacet, jmsKnob);
        invalidateCachedKnobs();
    }

    /**
     * Configure this Message as a Email Message.  This attaches a {@link javax.mail.internet.MimeMessage} to this
     * Message.  A Message may have at most one Email knob.
     *
     * @param emailKnob source of Email message.  May not be null.
     * @throws IllegalStateException if this Message is already configured as a JMS Message
     */
    public void attachEmailKnob(EmailKnob emailKnob) throws IllegalStateException {
        if (getKnob(EmailKnob.class) != null)
            throw new IllegalStateException("This Message is already configured as an Email Message");
        rootFacet = new EmailFacet(this, rootFacet, emailKnob);
        invalidateCachedKnobs();
    }

    /**
     * Configure this Message as an FTP Message.  This attaches an FTP STOR command to this
     * Message.  A Message may have at most one FTP knob.
     *
     * @param ftpRequestKnob source of FTP data.  May not be null.
     * @throws IllegalStateException if this Message is already configured as an FTP Message
     */
    public void attachFtpKnob(FtpRequestKnob ftpRequestKnob) throws IllegalStateException {
        if (getKnob(FtpRequestKnob.class) != null)
            throw new IllegalStateException("This Message is already configured as an FTP Message");
        rootFacet = new FtpFacet(this, rootFacet, ftpRequestKnob);
        invalidateCachedKnobs();
    }

    /**
     * Obtain the source for HTTP request transport metadata.  This assumes that this Message has already been
     * configured as an HTTP request by calling {@link #attachHttpRequestKnob}.
     *
     * @return an {@link HttpRequestKnob} ready to act as a source for HTTP request transport metadata.  Never null.
     * @throws IllegalStateException if this Message is not configured as an HTTP request.
     */
    public HttpRequestKnob getHttpRequestKnob() throws IllegalStateException {
        HttpRequestKnob knob = getKnob(HttpRequestKnob.class);
        if (knob == null)
            throw new IllegalStateException("This Message is not configured as an HTTP request");
        return knob;
    }

    /**
     * Configure this Message as an HTTP response.  This attaches a sink for HTTP response transport metadata to this
     * Message.  A message may have at most one HTTP response knob.  An HTTP response knob may cooexist with an
     * HTTP request knob.
     *
     * @param httpResponseKnob  the sink for HTTP response transport metadata.  May not be null.
     * @throws IllegalStateException if this Message is already configured as an HTTP response
     */
    public void attachHttpResponseKnob(HttpResponseKnob httpResponseKnob) throws IllegalStateException {
        if (getKnob(HttpResponseKnob.class) != null)
            throw new IllegalStateException("This Message is already configured as an HTTP response");
        rootFacet = new HttpResponseFacet(this, rootFacet, httpResponseKnob);
        invalidateCachedKnobs();
    }

    /**
     * Obtain the sink for HTTP response transport metadata.  This assumes that this Message has already been
     * configured as an HTTP response by calling {@link #attachHttpResponseKnob}.
     *
     * @return an {@link HttpResponseKnob} ready to act as a sink for HTTP response transport metadata.  Never null.
     * @throws IllegalStateException if this Message is not configured as an HTTP response
     */
    public HttpResponseKnob getHttpResponseKnob() throws IllegalStateException {
        HttpResponseKnob knob = getKnob(HttpResponseKnob.class);
        if (knob == null)
            throw new IllegalStateException("This Message is not configured as an HTTP response");
        return knob;
    }

    /**
     * Obtain the information about the TCP connection this message came over.  This assumes that this Message
     * has already been configured as having arrived via a TCP connection.
     *
     * @return the {@link TcpKnob}.  Never null
     * @throws IllegalStateException if this message is not configured as having arrived over TCP.
     */
    public TcpKnob getTcpKnob() throws IllegalStateException {
        TcpKnob knob = getKnob(TcpKnob.class);
        if (knob == null)
            throw new IllegalStateException("This Message is not configured as having arrived over TCP");
        return knob;
    }

    /**
     * Obtain information about the JMS message that produced this Message.  This assumes that this Message
     * has already been configured as having arrived over JMS.
     *
     * @return the {@link JmsKnob}.  Never null
     * @throws IllegalStateException if this message is not configured as having arrived over JMS.
     */
    public JmsKnob getJmsKnob() {
        JmsKnob knob = getKnob(JmsKnob.class);
        if (knob == null)
            throw new IllegalStateException("This Message is not configured as having arrived over JMS");
        return knob;
    }

    public void notifyMessage(Message message, MessageRole role) {
        relatedMessages.put(role, message);
    }

    public Message getRelated(MessageRole role) {
        return relatedMessages.get(role);
    }
    
    /**
     * Attach the specified knob to this message if and only if it does not already provide that knob.
     *
     * @param knobClass the class of the interface provided by this knob implementation.
     * @param knob the knob to attach.  It will be attached in a new facet.  Must not be null.
     * @throws IllegalStateException if this message already offers an implementation of the specified knobClass
     * @throws IllegalArgumentException if knob is not an instance of knobClass
     */
    public <T extends MessageKnob> void attachKnob(@NotNull Class<T> knobClass, @NotNull T knob) {
        if (getKnob(knobClass) != null)
            throw new IllegalStateException("An implementation of the knob " + knobClass + " is already attached to this Message.");
        if (!knobClass.isAssignableFrom(knob.getClass()))
            throw new IllegalArgumentException("knob was not an implementation of knobClass " + knobClass);
        rootFacet = new KnobHolderFacet(this, rootFacet, knobClass, knob);
        if (getKnob(knobClass) == null)
            throw new IllegalArgumentException("knob failed to provide an implementation of knobClass" + knobClass); // can't happen
    }

    /**
     * Attach a knob to this message that responds to the specified knob classes, if and only if
     * the message does not already provide any of these knob classes.
     *
     * @param knob the knob to attach.  It will be attached in a new facet.  Must not be null.
     * @param knobClasses the classes of the interface provided by this knob implementation.  Must be non-null and non-empty.
     * @throws IllegalStateException if this message already offers an implementation of the specified knobClass
     * @throws IllegalArgumentException if knob is not an instance of knobClass
     */
    public void attachKnob(@NotNull MessageKnob knob, @NotNull Class... knobClasses) {
        for (Class knobClass : knobClasses) {
            if (!knobClass.isAssignableFrom(knob.getClass()))
                throw new IllegalArgumentException("knob was not an implementation of knobClass " + knobClass);
            //noinspection unchecked
            if (getKnob(knobClass) != null)
                throw new IllegalStateException("An implementation of the knob " + knobClass + " is already attached to this Message.");
        }
        rootFacet = new KnobHolderFacet(this, rootFacet, knob, knobClasses);
    }


    /**
     * Get the specified knob, which must already be provided.
     *
     * @param c a Class derived from MessageKnob.  Must be non-null.
     * @return the requested MessageKnob.  Never null.
     * @throws IllegalStateException if no such knob is currently available from this Message.
     */
    @SuppressWarnings({"unchecked"})
    public <T extends MessageKnob> T getKnobAlways(Class<T> c) throws IllegalStateException {
        T got = getKnob(c);
        if (got == null)
            throw new IllegalStateException("This Message is not currently configured with " + c.getName());
        return got;
    }

    /**
     * If this Message has the specified knob, then return it.  Will not attempt to create any facets
     * that haven't yet been installed, even assuming it might be possible to do so.
     *
     * @param c a Class (usually) derived from MessageKnob.  Must be non-null.
     * @return the requested MessageKnob, if its facet is installed on this Message, or null.
     */
    @SuppressWarnings({"unchecked"})
    public <T> T getKnob(Class<T> c) {
        if (c == null) throw new NullPointerException();
        if (rootFacet == null) return null;

        // These knobs account for at least 2/3rds of the dozens of calls to this method that are made
        // per request.  Traversing the knob list so much was starting to show up in the profile.
        if (c == MimeKnob.class)
            return mimeKnob != null ? (T)mimeKnob : (T)(mimeKnob = (MimeKnob)findKnob(c));
        if (c == HttpRequestKnob.class)
            return httpRequestKnob != null ? (T)httpRequestKnob : (T)(httpRequestKnob = (HttpRequestKnob)findKnob(c));
        if (c == HttpServletRequestKnob.class)
            return httpServletRequestKnob != null ? (T)httpServletRequestKnob : (T)(httpServletRequestKnob = (HttpServletRequestKnob)findKnob(c));
        if (c == HttpResponseKnob.class)
            return httpResponseKnob != null ? (T)httpResponseKnob : (T)(httpResponseKnob = (HttpResponseKnob)findKnob(c));
        if (c == HttpServletResponseKnob.class)
            return httpServletResponseKnob != null ? (T)httpServletResponseKnob : (T)(httpServletResponseKnob = (HttpServletResponseKnob)findKnob(c));
        if (c == TcpKnob.class)
            return tcpKnob != null ? (T)tcpKnob : (T)(tcpKnob = (TcpKnob)findKnob(c));
        if (c == XmlKnob.class)
            return xmlKnob != null ? (T)xmlKnob : (T)(xmlKnob = (XmlKnob)findKnob(c));
        if (c == SecurityKnob.class)
            return securityKnob != null ? (T)securityKnob : (T)(securityKnob = (SecurityKnob)findKnob(c));
        if (c == SoapKnob.class)
            return soapKnob != null ? (T)soapKnob : (T)(soapKnob = (SoapKnob)findKnob(c));
        if (c == JsonKnob.class)
            return jsonKnob != null ? (T)jsonKnob : (T)(jsonKnob = (JsonKnob)findKnob(c));

        return (T)findKnob(c);
    }

    private MessageKnob findKnob(Class c) {
        return rootFacet.getKnob(c);
    }

    private void invalidateCachedKnobs() {
        httpRequestKnob = null;
        httpServletRequestKnob = null;
        httpResponseKnob = null;
        httpServletResponseKnob = null;
        tcpKnob = null;
        xmlKnob = null;
        soapKnob = null;
        mimeKnob = null;
        securityKnob = null;
        jsonKnob = null;
    }

    public static boolean isDefaultEnableOriginalDocument() {
        return defaultEnableOriginalDocument;
    }

    public static void setDefaultEnableOriginalDocument(Object defaultEnableOriginalDocument) {
        Message.defaultEnableOriginalDocument = Boolean.valueOf(String.valueOf(defaultEnableOriginalDocument));
    }

    /**
     * Free any resources being used by this Message or any of its facets.
     */
    @Override
    public void close() {
        try {
            if (rootFacet != null)
                rootFacet.close();
        } finally {
            rootFacet = null;
            initialized = false;
            invalidateCachedKnobs();
        }
    }

    /**
     * Notify any knobs attached to this Message that any cached representations of message data they may hold may now
     * be invalid (i.e. the document may have mutated out from under the cache)
     */
    void invalidateCaches() {
        TarariKnob tk = getKnob(TarariKnob.class);
        if (tk != null) tk.close();
        SoapKnob sk = getKnob(SoapKnob.class);
        if (sk != null) sk.invalidate();
    }

    /**
     * Get an ordered list of all current message facets that extend PreservableFacet.
     *
     * @return an order list of PreservableFacet instances.  May be empty but never null.
     */
    @NotNull
    List<PreservableFacet> getPreservableFacets() {
        List<PreservableFacet> preservables = new ArrayList<PreservableFacet>();
        if (rootFacet == null)
            return preservables;
        return rootFacet.visitFacets(new Functions.Binary<List<PreservableFacet>, MessageFacet, List<PreservableFacet>>() {
            @Override
            public List<PreservableFacet> call(MessageFacet messageFacet, List<PreservableFacet> preservables) {
                if (messageFacet instanceof PreservableFacet)
                    preservables.add((PreservableFacet) messageFacet);
                return preservables;
            }
        }, preservables);
    }
}
