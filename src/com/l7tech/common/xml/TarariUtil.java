/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.xml;

import com.l7tech.common.util.SoapUtil;
import com.tarari.xml.XMLDocumentException;
import com.tarari.xml.XMLErrorCode;
import com.tarari.xml.xpath.XPathCompiler;
import com.tarari.xml.xpath.XPathCompilerException;
import com.tarari.xml.xpath.XPathLoaderException;
import com.tarari.xml.xpath.XPathProcessorException;
import org.xml.sax.SAXException;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author alex
 * @version $Revision$
 */
public class TarariUtil {
    public static final String ENABLE_PROPERTY = "com.l7tech.common.xml.tarari.enable";
    public static final String XPATH_COMPILER_CLASSNAME = "com.tarari.xml.xpath.XPathCompiler";
    private static Boolean present = null;
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

    public static boolean isTarariPresent() {
        if (present == null) {
            if (Boolean.getBoolean(ENABLE_PROPERTY)) {
                try {
                    Class xpathCompilerClass = Class.forName(XPATH_COMPILER_CLASSNAME, false, TarariUtil.class.getClassLoader());
                    Method resetMethod = xpathCompilerClass.getMethod("reset", new Class[0]);
                    resetMethod.invoke(null, new Object[0]);
                    present = Boolean.TRUE;
                } catch (UnsatisfiedLinkError e) {
                    present = Boolean.FALSE;
                } catch (Throwable t) {
                    present = Boolean.FALSE;
                }
            } else {
                // Disabled -- skip the probe
                present = Boolean.FALSE;
            }
        }
        return present.booleanValue();
    }

    /** Thrown when a document cannot be processed in hardware for some reason but should be retried in software. */
    public static class SoftwareFallbackException extends Exception {
        public SoftwareFallbackException() {
        }

        public SoftwareFallbackException(String message) {
            super(message);
        }

        public SoftwareFallbackException(Throwable cause) {
            super(cause);
        }

        public SoftwareFallbackException(String message, Throwable cause) {
            super(message, cause);
        }
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

    public static void setupIsSoap() throws XPathCompilerException {
        if (!isTarariPresent()) throw new IllegalStateException("No Tarari card present");

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

        XPathCompiler.compile(xpaths0, 0);
    }
}
