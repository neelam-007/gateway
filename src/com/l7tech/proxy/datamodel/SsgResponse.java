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
import com.l7tech.proxy.attachments.ClientMultipartMessageReader;
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
public class SsgResponse {
    private static final Logger log = Logger.getLogger(SsgResponse.class.getName());
    final private Document responseDoc;
    final private HttpHeaders headers;
    final private int httpStatus;
    final private ProcessorResult processorResult;
    private String responseString = null;
    private ClientMultipartMessageReader multipartReader;
    private PostMethod downstreamPostMethod = null;

    private transient Boolean isSoap = null;
    private transient Boolean isFault = null;

    // Fields that are only meaningful if this is a fault
    private transient String faultcode = null;
    private transient String faultstring = null;
    private transient Element faultdetail = null;
    private transient String faultactor = null;

    public SsgResponse(Document wssProcessedResponse, ProcessorResult wssProcessorResult,
                       int httpStatus, HttpHeaders headers, ClientMultipartMessageReader multipartReader)
    {
        if (wssProcessedResponse == null) throw new IllegalArgumentException("response document must be non-null");
        this.responseDoc = wssProcessedResponse;
        this.httpStatus = httpStatus;
        this.headers = headers;
        this.processorResult = wssProcessorResult;
        this.multipartReader = multipartReader;
    }

    /** @return true if this SSG response appears to be a SOAP message. */
    public boolean isSoap() {
        if (isSoap == null)
            isSoap = Boolean.valueOf(SoapUtil.isSoapMessage(responseDoc));
        return isSoap.booleanValue();
    }

    /** @return true if this SSG response appears to be a SOAP fault. */
    public boolean isFault() {
        if (isFault == null) {
            if (!isSoap()) {
                isFault = Boolean.FALSE;
            } else {
                try {
                    Element payload = SoapUtil.getPayloadElement(responseDoc);
                    if (payload != null && "Fault".equals(payload.getLocalName()) && payload.getNamespaceURI().equals(responseDoc.getDocumentElement().getNamespaceURI())) {
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

    public PostMethod getDownstreamPostMethod() {
        return downstreamPostMethod;
    }

    public void setDownstreamPostMethod(PostMethod downstreamPostMethod) {
        this.downstreamPostMethod = downstreamPostMethod;
    }

    public static SsgResponse makeFaultResponse(String faultCode, String faultString, String faultActor) {
        try {
            String responseString = SoapFaultUtils.generateRawSoapFault(faultCode,
                                                                        faultString,
                                                                        "",
                                                                        "");
            HttpHeaders headers = new HttpHeaders(new Header[0]);
            return new SsgResponse(XmlUtil.stringToDocument(responseString), null, 500, headers, null);
        } catch (IOException e) {
            throw new RuntimeException(e); // can't happen
        }  catch (SAXException e) {
            throw new RuntimeException(e); // can't happen
        }
    }

    public int getHttpStatus() {
        return this.httpStatus;
    }

    public ClientMultipartMessageReader getMultipartReader() {
        return multipartReader;
    }

    public String getResponseAsString() throws IOException {
        if (responseString != null) return responseString;
        return responseString = XmlUtil.nodeToString(responseDoc);
    }

    public Document getResponseAsDocument() throws IOException, SAXException {
        return responseDoc;
    }

    /**
     * Returns the response as a Document, if one is already available, or as a String otherwise.  Use
     * to avoid parsing when possible, if you don't prefer either format.
     *
     * @return
     */
    public Object getResponseFast() {
        if (responseDoc != null)
            return responseDoc;
        return responseString;
    }

    public HttpHeaders getResponseHeaders() {
        return headers;
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
}
