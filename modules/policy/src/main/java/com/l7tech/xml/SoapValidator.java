/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.xml;

import com.l7tech.util.SoapConstants;

/**
 * Class that uses CompiledXpaths to ensure that a SOAP envelope is valid.
 */
public class SoapValidator {
    /**
     * Tests whether a given document is a valid SOAP envelope.
     *
     * @param c  An ElementCursor open anywhere on the document to examine.  Its initial position does not matter.
     *           Must not be null.  Where the cursor will be positioned when this
     *           method returns is not defined: caller must save the position themselves if they wish to preserve it.
     * @return null if c is a valid SOAP envelope, or a brief one-line phrase explaining why it isn't
     */
    public static String validateSoapMessage(ElementCursor c) {
        if (c == null) throw new IllegalArgumentException("An ElementCursor must be provided");
        c.moveToDocumentElement();
        if (!"Envelope".equals(c.getLocalName()))
            return "Document element is not Envelope";
        String soapNs = c.getNamespaceUri();
        if (soapNs == null || soapNs.length() < 1 || !SoapConstants.ENVELOPE_URIS.contains(soapNs))
            return "Envelope element was present but the namespace was not recognized as SOAP";

        if (c.containsMixedModeContent(true, false))
            return "Envelope element was present but contained mixed mode content";

        boolean sawBody = false;
        boolean sawHeader = false;

        if (!c.moveToFirstChildElement())
            return "Envelope element was empty";

        do {
            String ns = c.getNamespaceUri();
            if (!soapNs.equals(ns))
                return "Envelope contains a subelement whose namespace URI does not match that of the Envelope";
            String name = c.getLocalName();
            if ("Body".equals(name)) {
                if (sawBody)
                    return "Envelope contains more than one Body subelement";
                sawBody = true;
            } else if ("Header".equals(name)) {
                if (sawHeader)
                    return "Envelope contains more than one Header subelement";
                if (sawBody)
                    return "Envelope contains a Header sublement that comes after the Body subelement";
                sawHeader = true;
            } else
                return "Envelope contains a subelement that is neither Header nor Body";
        } while (c.moveToNextSiblingElement());

        // Looks good.
        return null;
    }
}
