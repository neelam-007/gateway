/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.server.tarari;

import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.xml.InvalidXpathException;
import com.l7tech.common.xml.tarari.GlobalTarariContext;
import com.l7tech.common.xml.tarari.TarariUtil;
import com.tarari.xml.xpath.XPathCompiler;
import com.tarari.xml.xpath.XPathCompilerException;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Holds the server-side Tarari state
 */
public class GlobalTarariContextImpl implements GlobalTarariContext {
    private Xpaths currentXpaths = buildDefaultXpaths();

    public void compile() {
        while (true) {
            try {
                String[] expressions = currentXpaths.getExpressions();
                XPathCompiler.compile(expressions, 0);
                currentXpaths.installed(expressions);
                return; // No exception, we're done
            } catch (XPathCompilerException e) {
                int badIndex = e.getCompilerErrorLine() - 1; // Silly Tarari, 1-based arrays are for kids!
                currentXpaths.remove(currentXpaths.getExpression(badIndex));
                if (currentXpaths.getExpression(0) == null)
                    throw new IllegalStateException("Last XPath was removed without successful compilation");
            }
        }
    }

    public void add(String expression) throws InvalidXpathException {
        currentXpaths.add(expression);
    }

    public void remove(String expression) {
        currentXpaths.remove(expression);
    }

    public int getIndex(String expression) {
        return currentXpaths.getIndex(expression) + 1;
    }

    /**
     * Builds the initial {@link Xpaths}
     */
    private Xpaths buildDefaultXpaths() {
        // Built-in stuff for isSoap etc.
        ArrayList xpaths0 = new ArrayList(TarariUtil.ISSOAP_XPATHS.length + 10);
        xpaths0.addAll(Arrays.asList(TarariUtil.ISSOAP_XPATHS));
        int ursStart = xpaths0.size() + 1; // 1-based arrays
        int[] uriIndices = new int[SoapUtil.ENVELOPE_URIS.size()+1];
        for (int i = 0; i < SoapUtil.ENVELOPE_URIS.size(); i++) {
            String uri = (String)SoapUtil.ENVELOPE_URIS.get(i);
            String nsXpath = TarariUtil.NS_XPATH_PREFIX + uri + TarariUtil.NS_XPATH_SUFFIX;
            xpaths0.add(nsXpath);
            uriIndices[i] = i + ursStart;
        }
        uriIndices[uriIndices.length-1] = 0;

        return new Xpaths(xpaths0, uriIndices);
    }

    public int[] getSoapNamespaceUriIndices() {
        return currentXpaths.getSoapUriIndices();
    }
}
