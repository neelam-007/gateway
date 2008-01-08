/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.xml.tarari;

import com.l7tech.common.util.Functions;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Holds a {@link com.tarari.xml.xslt11.Stylesheet} instance, but in a way that introduces no static dependencies
 * on any Tarari classes in SSG code that is running without any Tarari classes present (in that situation,
 * all TarariCompiledStylesheet references would be null).
 */
public interface TarariCompiledStylesheet {
    /**
     * Perform a Tarari XSLT transformation upon an already-created TarariMessageContext.
     *
     * @param input   an already-parsed TarariMessageContext to transform.  Must not be null.
     * @param output  stream to which the transformation output will be written.  Must not be null.
     * @param varsUsed  names of variables used by this stylesheet, or null.
     * @param variableGetter  variableGetter to look up variables used by this stylesheet, or null.
     * @throws IOException if there is a problem writing to output
     * @throws SAXException if the input document can't be parsed.  (Probably can't happen here.)
     */
    void transform(TarariMessageContext input, OutputStream output, String[] varsUsed, Functions.Unary<Object, String> variableGetter) throws IOException, SAXException;

    /**
     * Perform a Tarari XSLT transformation upon the specified XML message stream.
     *
     * @param input   stream containing an XML message to transform.  Must not be null.
     * @param output  stream to which the transformation output will be written.  Must not be null.
     * @param varsUsed  names of variables used by this stylesheet, or null.
     * @param variableGetter  variableGetter to look up variables used by this stylesheet, or null.
     * @throws IOException if the input document can't be read.
     * @throws SAXException if the input document can't be parsed.
     * @throws IOException if there is a problem writing to output
     */
    void transform(InputStream input, OutputStream output, String[] varsUsed, Functions.Unary<Object, String> variableGetter) throws SAXException, IOException;
}
