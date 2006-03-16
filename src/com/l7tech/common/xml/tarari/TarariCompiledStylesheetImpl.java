/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.xml.tarari;

import com.l7tech.common.io.EmptyInputStream;
import com.l7tech.common.util.ExceptionUtils;
import com.tarari.xml.XmlParseException;
import com.tarari.xml.XmlResult;
import com.tarari.xml.XmlSource;
import com.tarari.xml.rax.RaxDocument;
import com.tarari.xml.xslt11.Stylesheet;
import com.tarari.xml.xslt11.parser.XsltParseException;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;

/**
 * Implementation of {@link TarariCompiledStylesheet}.  Unlike its parent interface, this class will only be loaded
 * if Tarari is actually present, so it is able to safely reference Tarari classes directly.
 */
public class TarariCompiledStylesheetImpl implements TarariCompiledStylesheet {
    private final Stylesheet master;

    // Thread-local XmlSource for transformations.
    private static final ThreadLocal xmlSource = new ThreadLocal() {
        protected Object initialValue() {
            return new XmlSource(new EmptyInputStream());
        }
    };

    /**
     * Wrap the specified Tarari Stylesheet instance.
     *
     * @param xslBytes   the stylesheet to compile.  Must not be null.
     * @throws ParseException  if the specified stylesheet bytes could not be compiled.
     */
    TarariCompiledStylesheetImpl(byte[] xslBytes) throws ParseException {
        try {
            master = Stylesheet.create(new XmlSource(xslBytes));
        } catch (XsltParseException e) {
            throw (ParseException)new ParseException("Unable to parse XSLT: " + ExceptionUtils.getMessage(e), 0).initCause(e);
        } catch (IOException e) {
            // Shouldn't be possible
            throw (ParseException)new ParseException("Unable to read XSLT: " + ExceptionUtils.getMessage(e), 0).initCause(e);
        } catch (XmlParseException e) {
            throw (ParseException)new ParseException("Unable to parse XSLT: " + ExceptionUtils.getMessage(e), 0).initCause(e);
        }
    }

    public void transform(TarariMessageContext input, OutputStream output) throws IOException, SAXException {
        XmlSource source = (XmlSource)xmlSource.get();
        RaxDocument raxDocument = ((TarariMessageContextImpl)input).getRaxDocument();
        source.setData(raxDocument);
        transform(source, output);
    }

    public void transform(InputStream input, OutputStream output) throws SAXException, IOException {
        transform(new XmlSource(input), output);
    }

    private void transform(XmlSource source, OutputStream output) throws IOException, SAXException {
        Stylesheet transformer = new Stylesheet(master);
        transformer.setValidate(false);
        XmlResult result = new XmlResult(output);
        try {
            transformer.transform(source, result);
        } catch (XmlParseException e) {
            throw new SAXException(e);
        }
    }
}
