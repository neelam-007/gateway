/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.server.tarari;

import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.xml.InvalidSchemaException;
import com.l7tech.common.xml.InvalidXpathException;
import com.l7tech.common.xml.tarari.GlobalTarariContext;
import com.l7tech.common.xml.tarari.SchemaHandle;
import com.l7tech.common.xml.tarari.TarariUtil;
import com.tarari.xml.schema.SchemaLoader;
import com.tarari.xml.schema.SchemaLoadingException;
import com.tarari.xml.xpath.XPathCompiler;
import com.tarari.xml.xpath.XPathCompilerException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Holds the server-side Tarari state
 */
public class GlobalTarariContextImpl implements GlobalTarariContext {
    private Xpaths currentXpaths = buildDefaultXpaths();
    private long compilerGeneration = 1;
    private Map schemaHandlesByUri = new HashMap();

    public void compile() {
        while (true) {
            try {
                String[] expressions = currentXpaths.getExpressions();
                XPathCompiler.compile(expressions, 0);
                currentXpaths.installed(expressions, nextCompilerGeneration());
                return; // No exception, we're done
            } catch (XPathCompilerException e) {
                int badIndex = e.getCompilerErrorLine() - 1; // Silly Tarari, 1-based arrays are for kids!
                currentXpaths.remove(currentXpaths.getExpression(badIndex));
                if (currentXpaths.getExpression(0) == null)
                    throw new IllegalStateException("Last XPath was removed without successful compilation");
            }
        }
    }

    private synchronized long nextCompilerGeneration() {
        return ++compilerGeneration;
    }

    public long getCompilerGeneration() {
        return compilerGeneration;
    }

    public void addXpath(String expression) throws InvalidXpathException {
        currentXpaths.add(expression);
    }

    public void removeXpath(String expression) {
        currentXpaths.remove(expression);
    }

    public void addSchema(String nsUri, String schema) throws InvalidSchemaException, IOException {
        SchemaHandle handle = null;
        try {
            synchronized (this) {
                handle = (SchemaHandle)schemaHandlesByUri.get(nsUri);
                if (handle == null) {
                    handle = new SchemaHandle(nsUri, schema);
                    schemaHandlesByUri.put(nsUri, handle);
                } else if (handle.inUse() && !schema.equals(handle.getSchemaDoc())) {
                    throw new InvalidSchemaException("A different schema with Namespace URI " + nsUri + " is currently in use!");
                }
            }
            handle.ref();
            SchemaLoader.loadSchema(new ByteArrayInputStream(schema.getBytes("UTF-8")), "");
            handle = null; // Prevent unref() in finally
        } catch (SchemaLoadingException e) {
            throw new InvalidSchemaException(e);
        } finally {
            if (handle != null) handle.unref();
        }
    }

    public void removeSchema(String nsUri) {
        SchemaHandle handle = null;

        synchronized (this) {
            handle = (SchemaHandle) schemaHandlesByUri.get(nsUri);
        }

        if (handle != null) {
            handle.unref();
            if (!handle.inUse()) {
                SchemaLoader.unloadSchema(nsUri);
            }
        }
    }

    public int getXpathIndex(String expression, long targetCompilerGeneration) {
        if (expression == null) return -1;
        return currentXpaths.getIndex(expression, targetCompilerGeneration) + 1;
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
