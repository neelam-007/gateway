/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.xml.tarari;

import com.l7tech.common.xml.SoftwareFallbackException;
import com.l7tech.common.xml.TarariProber;
import com.l7tech.common.util.SoapUtil;
import com.tarari.xml.XMLDocumentException;
import com.tarari.xml.XMLErrorCode;
import com.tarari.xml.xpath.XPathCompilerException;
import com.tarari.xml.xpath.XPathLoaderException;
import com.tarari.xml.xpath.XPathProcessorException;
import com.tarari.xml.xpath.XPathCompiler;
import org.xml.sax.SAXException;

import java.util.ArrayList;
import java.util.Arrays;

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

    public static final String NS_XPATH_PREFIX = "//*[namespace-uri()=\"";
    public static final String NS_XPATH_SUFFIX = "\"]";
    private static int[] uriIndices;

    public static int[] getUriIndices() {
        return uriIndices;
    }

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

    public static int[] setupIsSoap(String[] moreXpaths) throws XPathCompilerException {
        if (!TarariProber.isTarariPresent()) throw new IllegalStateException("No Tarari card present");

        ArrayList xpaths0 = new ArrayList();
        xpaths0.addAll(Arrays.asList(ISSOAP_XPATHS));
        int ursStart = xpaths0.size() + 1; // 1-based arrays
        uriIndices = new int[SoapUtil.ENVELOPE_URIS.size()+1];
        for (int i = 0; i < SoapUtil.ENVELOPE_URIS.size(); i++) {
            String uri = (String)SoapUtil.ENVELOPE_URIS.get(i);
            String nsXpath = NS_XPATH_PREFIX + uri + NS_XPATH_SUFFIX;
            xpaths0.add(nsXpath);
            uriIndices[i] = i + ursStart;
        }
        uriIndices[uriIndices.length-1] = 0;

        int before = xpaths0.size();
        int[] moreIndices = null;
        if (moreXpaths != null && moreXpaths.length > 0) {
            xpaths0.addAll(Arrays.asList(moreXpaths));
            moreIndices = new int[moreXpaths.length];
            for (int i = 0; i < moreIndices.length; i++) {
                moreIndices[i] = ++before;
            }
        }

        XPathCompiler.compile(xpaths0, 0);
        return moreIndices;
    }

    public static void setupIsSoap() throws XPathCompilerException {
        setupIsSoap(null);
    }

}
