/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.proxy.util.ClientLogger;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.apache.commons.httpclient.Header;

import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import java.io.IOException;

/**
 * Class encapsulating the response from the Ssg to a given request.  Does parsing on-demand.
 * User: mike
 * Date: Aug 15, 2003
 * Time: 9:48:45 AM
 */
public class SsgResponse {
    private static final ClientLogger log = ClientLogger.getInstance(SsgResponse.class);
    private String responseString = null;
    private Document responseDoc = null;
    private HttpHeaders headers;
    private int httpStatus = 0;

    public SsgResponse(String response, int httpStatus, HttpHeaders headers) {
        this.responseString = response;
        this.httpStatus = httpStatus;
        this.headers = headers;
    }

    public SsgResponse(Document response, int httpStatus, HttpHeaders headers) {
        this.responseDoc = response;
        this.httpStatus = httpStatus;
        this.headers = headers;
    }

    public static SsgResponse makeFaultResponse(String faultCode, String faultString, String faultActor) {
        SOAPMessage faultMessage = SoapUtil.makeFaultMessage(faultCode, faultString, faultActor);
        try {
            String responseString = new String(SoapUtil.soapMessageToByteArray(faultMessage));
            HttpHeaders headers = new HttpHeaders(new Header[0]);
            return new SsgResponse(responseString, 500, headers);
        } catch (IOException e) {
            throw new RuntimeException(e); // can't happen
        } catch (SOAPException e) {
            throw new RuntimeException(e); // can't happen
        }
    }

    public int getHttpStatus() {
        return this.httpStatus;
    }

    public String getResponseAsString() throws IOException {
        if (responseDoc != null)
            return XmlUtil.nodeToString(responseDoc);
        return responseString;
    }

    public Document getResponseAsDocument() throws IOException, SAXException {
        if (responseDoc != null)
            return responseDoc;
        if (responseString == null)
            return null;

        responseDoc = XmlUtil.stringToDocument(responseString);
        return responseDoc;
    }

    /**
     * Replace the response with a new document.
     * @param newResponse
     */
    public void setResponse(Document newResponse) {
        this.responseDoc = newResponse;
        this.responseString = null;
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
            log.error(e);
            return "<SsgResponse toString error: " + e + ">";
        }
    }
}
