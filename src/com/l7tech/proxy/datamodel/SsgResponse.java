/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

import com.l7tech.common.util.XmlUtil;
import org.apache.log4j.Category;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;

/**
 * Class encapsulating the response from the Ssg to a given request.  Does parsing on-demand.
 * User: mike
 * Date: Aug 15, 2003
 * Time: 9:48:45 AM
 */
public class SsgResponse {
    private static final Category log = Category.getInstance(SsgResponse.class);
    private String responseString = null;
    private Document responseDoc = null;

    public SsgResponse(String response) {
        this.responseString = response;
    }

    public SsgResponse(Document response) {
        this.responseDoc = response;
    }

    public String getResponseAsString() throws IOException {
        if (responseDoc != null)
            return XmlUtil.documentToString(responseDoc);
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

    public String toString() {
        try {
            return getResponseAsString();
        } catch (IOException e) {
            log.error(e);
            return "<SsgResponse toString error: " + e + ">";
        }
    }
}
