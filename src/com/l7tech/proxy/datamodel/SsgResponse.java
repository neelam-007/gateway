/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

import com.l7tech.common.security.xml.processor.ProcessorResult;
import com.l7tech.common.util.SoapFaultUtils;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.mime.MimeBody;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.methods.PostMethod;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class encapsulating the response from the Ssg to a given request.
 * User: mike
 * Date: Aug 15, 2003
 * Time: 9:48:45 AM
 */
public class SsgResponse extends ClientXmlMessage {
    private static final Logger log = Logger.getLogger(SsgResponse.class.getName());
    final private int httpStatus;
    final private ProcessorResult processorResult;
    private String responseString = null;
    private PostMethod downstreamPostMethod = null;

    private transient Boolean isFault = null;

    // Fields that are only meaningful if this is a fault
    private transient String faultcode = null;
    private transient String faultstring = null;
    private transient Element faultdetail = null;
    private transient String faultactor = null;

    /**
     * Create a new SsgResponse object with the specified values.
     *
     * @param mimeBody      a MimeBody managing any extra multipart parts that may have accompanied this response.
     *                              May not be null.
     * @param wssProcessedResponse  the response document, already processed by the WssProcessor if applicable.  May not be null.
     * @param wssProcessorResult    the result of running the response through the WssProcessor, or null if this wasn't done.
     * @param httpStatus            the HTTP result code of the response (ie, 500)
     * @param headers               the HTTP headers that came with the response, for logging purposes, or null.
     * @throws IOException          if originalDocument needs to be serialized, but cannot be, due to some
     *                              canonicalizer problem (relative namespaces, maybe)
     */
    public SsgResponse(MimeBody mimeBody, Document wssProcessedResponse, ProcessorResult wssProcessorResult,
                       int httpStatus, HttpHeaders headers)
            throws IOException
    {
        super(mimeBody, wssProcessedResponse, headers);
        if (mimeBody == null) throw new NullPointerException("mimeBody non-null");
        if (wssProcessedResponse == null) throw new NullPointerException("wssProcessedResponse must be non-null");
        this.httpStatus = httpStatus;
        this.processorResult = wssProcessorResult;
    }

    /** @return true if this SSG response appears to be a SOAP fault. */
    public boolean isFault() {
        if (isFault == null) {
            if (!isSoap()) {
                isFault = Boolean.FALSE;
            } else {
                try {
                    Element payload = SoapUtil.getPayloadElement(getOriginalDocument());
                    if (payload != null && "Fault".equals(payload.getLocalName()) &&
                        payload.getNamespaceURI().equals(getOriginalDocument().getDocumentElement().getNamespaceURI()))
                    {
                        Element faultcodeEl = XmlUtil.findFirstChildElementByName(payload, (String)null, "faultcode");
                        if (faultcodeEl != null)
                            faultcode = XmlUtil.getTextValue(faultcodeEl);
                        Element faultstringEl = XmlUtil.findFirstChildElementByName(payload, (String)null, "faultstring");
                        if (faultstringEl != null)
                            faultstring = XmlUtil.getTextValue(faultstringEl);
                        Element faultactorEl = XmlUtil.findFirstChildElementByName(payload, (String)null, "faultactor");
                        if (faultactorEl != null)
                            faultactor = XmlUtil.getTextValue(faultactorEl);
                        faultdetail = XmlUtil.findFirstChildElementByName(payload, (String)null, "faultdetail");
                        log.info("This Gateway response appears to be a SOAP fault, with faultcode " + faultcode);
                        isFault = Boolean.TRUE;
                    } else
                        isFault = Boolean.FALSE;
                } catch (InvalidDocumentFormatException e) {
                    isFault = Boolean.FALSE;
                }
            }
        }
        return isFault.booleanValue();
    }

    public String getFaultcode() {
        if (!isFault()) return null;
        return faultcode;
    }

    public String getFaultstring() {
        if (!isFault()) return null;
        return faultstring;
    }

    public String getFaultactor() {
        if (!isFault()) return null;
        return faultactor;
    }

    public Element getFaultdetail() {
        if (!isFault()) return null;
        return faultdetail;
    }

    /**
     * Check if a PostMethod will be released by this SsgResponse when it is closed.
     *
     * @return true iff. this SsgResponse is holding a PostMethod that will be released on {@link #close}.
     */
    public boolean isDownstreamPostMethod() {
        return downstreamPostMethod != null;
    }

    /**
     * Transfer ownership of a PostMethod to this SsgResponse so it will be closed when the response is
     * finished being processed.
     * <p>
     * If the reponse is being streamed from a live InputStream from a PostMethod, the PostMethod
     * cannot safely be released until the response has been fully streamed.  In this situation, call this
     * method to transfer ownership of the PostMethod to this SsgResponse.  Then, the PostMethod will be
     * released when this response is closed, after being processed.
     *
     * @param downstreamPostMethod a PostMethod to release on close, or null to avoid doing so.
     */
    public void setDownstreamPostMethod(PostMethod downstreamPostMethod) {
        this.downstreamPostMethod = downstreamPostMethod;
    }

    public static SsgResponse makeFaultResponse(String faultCode, String faultString, String faultActor) {
        try {
            String responseString = SoapFaultUtils.generateRawSoapFault(faultCode,
                                                                        faultString,
                                                                        null,
                                                                        faultActor);
            HttpHeaders headers = new HttpHeaders(new Header[0]);
            final Document soapEnvelope = XmlUtil.stringToDocument(responseString);
            final byte[] xmlBytes = XmlUtil.nodeToString(soapEnvelope).getBytes();
            final MimeBody mimeBody = new MimeBody(xmlBytes, ContentTypeHeader.XML_DEFAULT);
            return new SsgResponse(mimeBody,
                                   soapEnvelope, null, 500, headers);
        } catch (IOException e) {
            throw new RuntimeException(e); // can't happen
        }  catch (SAXException e) {
            throw new RuntimeException(e); // can't happen
        } catch (NoSuchPartException e) {
            throw new RuntimeException(e); // can't happen
        }
    }

    public int getHttpStatus() {
        return this.httpStatus;
    }

    public String getResponseAsString() throws IOException {
        if (responseString != null) return responseString;
        return responseString = XmlUtil.nodeToString(getOriginalDocument());
    }

    public String toString() {
        try {
            return getResponseAsString();
        } catch (IOException e) {
            log.log(Level.SEVERE, e.getMessage(), e);
            return "<SsgResponse toString error: " + e + ">";
        }
    }

    /**
     * Get the result of running this response through WssProcessor.  Might be null.
     * @return The ProcessorResult, or null if the WssProcessor was not given or could not process the response.
     */
    public ProcessorResult getProcessorResult() {
        return processorResult;
    }

    /**
     * Free any resources being used by this SsgResponse.  If a downstream PostMethod has been set,
     * it will be released here.
     */
    public void close() {
        super.close();
        if (downstreamPostMethod != null) {
            downstreamPostMethod.releaseConnection();
            downstreamPostMethod = null;
        }
    }

    protected void finalize() throws Throwable {
        super.finalize();
        close();
    }
}
