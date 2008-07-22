/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.xml.tarari;

import com.tarari.xml.XmlParseException;
import com.tarari.xml.XmlConfigException;
import com.tarari.xml.XmlException;
import com.tarari.xml.rax.RaxErrorCode;
import com.l7tech.xml.SoftwareFallbackException;
import org.xml.sax.SAXException;

/**
 * @author alex
 */
class TarariUtil {
    public static final String[] ISSOAP_XPATHS = {
        // Don't change the first five, they're important for Tarari's magical isSoap() method
        "/*[local-name()=\"Envelope\"]",
        "/*[local-name()=\"Envelope\"]/*[1][local-name()=\"Header\"]",
        "/*[local-name()=\"Envelope\"]/*[local-name()=\"Body\"]",
        "/*[local-name()=\"Envelope\"]/*[1][local-name()=\"Header\"]/*[namespace-uri()=\"\"]",
        "/*/*",

        // This is our stuff (don't forget that Tarari uses 1-based arrays though)
        /* [6] payload element */
        "/*[local-name()=\"Envelope\"]/*[local-name()=\"Body\"]/*",
        /* [7] Security header          */
        "/*[local-name()=\"Envelope\"]/*[1][local-name()=\"Header\"]/*[local-name()=\"Security\"]"
    };

    /* Index of our own SOAP payload element xpath.  1-based index, ready to pass to RAXContext.getCount(). */
    public static final int XPATH_INDEX_PAYLOAD = 6;

    /* Index of our own Security header element xpath.  1-based index, ready to pass to RAXContext.getCount(). */
    public static final int XPATH_INDEX_SECHEADER = 7;

    public static final String NS_XPATH_PREFIX = "//*[namespace-uri()=\"";
    public static final String NS_XPATH_SUFFIX = "\"]";

    public static void translateTarariErrorCode(Exception cause, int code)
            throws SAXException, SoftwareFallbackException, RuntimeException
    {
        switch (code) {
            case RaxErrorCode.CANNOT_TOKENIZE:
            case RaxErrorCode.XPATH_UNDECLARED_PREFIX:
            case RaxErrorCode.PREFIX_NOT_FOUND:
            case RaxErrorCode.NAMESPACE_NOT_FOUND:
                throw new SAXException(cause);
        }
        throw new SoftwareFallbackException(cause);
    }

    public static void translateException(XmlParseException e) throws SAXException {
        throw new SAXException(e);
    }

    public static void translateException(XmlConfigException e) throws SoftwareFallbackException, SAXException {
        translateTarariErrorCode(e, e.getErrorCode());
    }

    public static void translateException(XmlException e) throws SoftwareFallbackException, SAXException {
        translateTarariErrorCode(e, e.getErrorCode());
    }
}
