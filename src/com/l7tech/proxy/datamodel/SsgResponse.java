/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

import com.l7tech.common.security.xml.WssProcessor;
import com.l7tech.common.util.SoapFaultUtils;
import com.l7tech.common.util.XmlUtil;
import org.apache.commons.httpclient.Header;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class encapsulating the response from the Ssg to a given request.  Does parsing on-demand.
 * User: mike
 * Date: Aug 15, 2003
 * Time: 9:48:45 AM
 */
public class SsgResponse {
    private static final Logger log = Logger.getLogger(SsgResponse.class.getName());
    private Document responseDoc = null;
    private String responseString = null;
    private HttpHeaders headers;
    private int httpStatus = 0;
    private WssProcessor.ProcessorResult processorResult = null;

    public SsgResponse(Document wssProcessedResponse, WssProcessor.ProcessorResult wssProcessorResult,
                       int httpStatus, HttpHeaders headers)
    {
        if (wssProcessedResponse == null) throw new IllegalArgumentException("response document must be non-null");
        this.responseDoc = wssProcessedResponse;
        this.httpStatus = httpStatus;
        this.headers = headers;
        this.processorResult = wssProcessorResult;
    }

    public static SsgResponse makeFaultResponse(String faultCode, String faultString, String faultActor) {
        try {
            String responseString = SoapFaultUtils.generateRawSoapFault(faultCode,
                                                                        faultString,
                                                                        "",
                                                                        SoapFaultUtils.FC_SERVER);
            HttpHeaders headers = new HttpHeaders(new Header[0]);
            return new SsgResponse(XmlUtil.stringToDocument(responseString), null, 500, headers);
        } catch (IOException e) {
            throw new RuntimeException(e); // can't happen
        }  catch (SAXException e) {
            throw new RuntimeException(e); // can't happen
        }
    }

    public int getHttpStatus() {
        return this.httpStatus;
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
    public WssProcessor.ProcessorResult getProcessorResult() {
        return processorResult;
    }
}
