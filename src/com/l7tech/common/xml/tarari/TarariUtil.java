/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.xml.tarari;

import com.l7tech.common.xml.SoftwareFallbackException;
import com.tarari.xml.XMLDocumentException;
import com.tarari.xml.XMLErrorCode;
import com.tarari.xml.xpath.XPathCompilerException;
import com.tarari.xml.xpath.XPathLoaderException;
import com.tarari.xml.xpath.XPathProcessorException;
import org.xml.sax.SAXException;

/**
 * @author alex
 * @version $Revision$
 */
public class TarariUtil {
    public static final String[] ISSOAP_XPATHS = {
        // Don't change the first five, they're important for Tarari's magical isSoap() method
        "/*[local-name()=\"Envelope\"]",
        "/*[local-name()=\"Envelope\"]/*[1][local-name()=\"Header\"]",
        "/*[local-name()=\"Envelope\"]/*[local-name()=\"Body\"]",
        "/*[local-name()=\"Envelope\"]/*[1][local-name()=\"Header\"]/*[namespace-uri()=\"\"]",
        "/*/*",

        // This is our stuff (don't forget that Tarari uses 1-based arrays though)
        /* [6] payload element */
        "/*[local-name()=\"Envelope\"]/*[local-name()=\"Body\"]/*[1]",
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
            case XMLErrorCode.CANNOT_TOKENIZE:
            case XMLErrorCode.XPATH_UNDECLARED_PREFIX:
            case XMLErrorCode.PREFIX_NOT_FOUND:
            case XMLErrorCode.NAMESPACE_NOT_FOUND:
                throw new SAXException(cause);
        }
        throw new SoftwareFallbackException(cause);
    }

    public static void translateException(XPathCompilerException e) 
            throws SAXException, SoftwareFallbackException, RuntimeException
    {
        translateTarariErrorCode(e, e.getCompilerErrorCode());
    }

    public static void translateException(XPathLoaderException e)
            throws SAXException, SoftwareFallbackException, RuntimeException
    {
        translateTarariErrorCode(e, e.getStatus());
    }

    public static void translateException(XPathProcessorException e)
            throws SAXException, SoftwareFallbackException, RuntimeException
    {
        translateTarariErrorCode(e, e.getStatus());
    }

    public static void translateException(XMLDocumentException e)
            throws SAXException, SoftwareFallbackException, RuntimeException
    {
        translateTarariErrorCode(e, e.getStatus());
    }

}
