/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

import org.apache.log4j.Category;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Class encapsulating the response from the Ssg to a given request.
 * User: mike
 * Date: Aug 15, 2003
 * Time: 9:48:45 AM
 */
public class SsgResponse {
    private static final Category log = Category.getInstance(SsgResponse.class);
    private String stringVersion = null;
    private SOAPEnvelope soapEnvelope = null;

    public SsgResponse(String response) {
        this.stringVersion = response;
    }

    public SsgResponse(SOAPEnvelope response) {
        this.soapEnvelope = response;
    }

    public String getResponseAsString() {
        if (soapEnvelope != null)
            return soapEnvelope.toString();
        return stringVersion;
    }

    public SOAPEnvelope getResponseAsSoapEnvelope() throws SOAPException {
        if (soapEnvelope != null)
            return soapEnvelope;
        if (stringVersion == null)
            return null;

        try {
            soapEnvelope = MessageFactory.newInstance().createMessage(null, new ByteArrayInputStream(stringVersion.getBytes())).getSOAPPart().getEnvelope();
        } catch (IOException e) {
            log.error("Impossible error", e);  // can't happen
            return null;
        }
        stringVersion = null;
        return soapEnvelope;
    }

    public String toString() {
        return getResponseAsString();
    }
}
