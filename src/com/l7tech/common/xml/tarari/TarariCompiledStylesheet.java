/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.xml.tarari;

import org.xml.sax.SAXException;

import java.io.OutputStream;
import java.io.InputStream;
import java.io.IOException;

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
     * @throws IOException if there is a problem writing to output
     * @throws SAXException if the input document can't be parsed.  (Probably can't happen here.)
     */
    void transform(TarariMessageContext input, OutputStream output) throws IOException, SAXException;

    /**
     * Perform a Tarari XSLT transformation upon the specified XML message stream.
     *
     * @param input   stream containing an XML message to transform.  Must not be null.
     * @param output  stream to which the transformation output will be written.  Must not be null.
     * @throws IOException if the input document can't be read.
     * @throws SAXException if the input document can't be parsed.
     * @throws IOException if there is a problem writing to output
     */
    void transform(InputStream input, OutputStream output) throws SAXException, IOException;
}
