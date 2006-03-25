/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.xml.tarari;

import com.l7tech.common.util.ArrayUtils;
import com.l7tech.common.xml.ElementCursor;
import com.l7tech.common.xml.xpath.CompiledXpath;
import com.l7tech.common.xml.xpath.XpathResult;
import com.tarari.io.Encoding;
import com.tarari.xml.XmlResult;
import com.tarari.xml.cursor.XmlCursor;
import com.tarari.xml.output.DomOutput;
import com.tarari.xml.output.OutputFormat;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.io.OutputStream;

/**
 * An {@link ElementCursor} implementation that wraps a Tarari XmlCursor and provides features tuned for the
 * read-only use of a WSS processor, but not so much for generic XML processing.
 */
class TarariElementCursor extends ElementCursor {
    private final TarariMessageContextImpl tarariMessageContext;
    private XmlCursor c;

    public TarariElementCursor(XmlCursor xmlCursor, TarariMessageContextImpl tarariMessageContext) {
        if (xmlCursor == null) throw new IllegalArgumentException("An XmlCursor must be provided");
        this.c = xmlCursor;
        this.tarariMessageContext = tarariMessageContext;
        c.toDocumentElement(); // Make sure it's pointing at an element
    }

    /**
     * Get the XmlCursor.  Package private.
     * @return the XmlCursor.  Never null.
     */
    XmlCursor getXmlCursor() {
        return c;
    }

    /**
     * Get the current TarariMessageContextImpl.  Package private.
     * @return the TarariMessageContextImpl.  Never null.
     */
    TarariMessageContextImpl getTarariMessageContext() {
        return tarariMessageContext;
    }

    public ElementCursor duplicate() {
        return new TarariElementCursor(c.duplicate(), tarariMessageContext);
    }

    public void pushPosition() {
        c.pushPosition();
    }

    public void popPosition() throws IllegalStateException {
        c.popPosition();
    }

    public void popPosition(boolean discard) throws IllegalStateException {
        c.popPosition(discard);
    }

    public void moveToDocumentElement() {
        c.toDocumentElement();
    }

    public void moveToRoot() {
        c.toRoot();
    }

    public boolean moveToParentElement() {
        boolean b = c.toParent();
        if (!b) return false;
        if (c.getNodeType() == XmlCursor.ROOT) {
            c.toDocumentElement();
            return false;
        }
        return b;
    }

    public boolean moveToFirstChildElement() {
        return c.toFirstChild(XmlCursor.ELEMENT);
    }

    public boolean moveToNextSiblingElement() {
        return c.toNextSibling(XmlCursor.ELEMENT);
    }

    public boolean moveToNextSiblingElement(String localName, String[] namespaceUris) {
        pushPosition();
        for (;;) {
            if (!c.toNextSibling(XmlCursor.ELEMENT)) {
                popPosition();
                return false;
            }
            if (localName.equals(c.getNodeLocalName()) &&
                    ArrayUtils.contains(namespaceUris, c.getNodeNamespaceUri()))
            {
                return true;
            }
        }
        /* NOTREACHED */
    }

    public boolean moveToNextSiblingElement(String localName, String namespaceUri) {
        pushPosition();
        for (;;) {
            if (!c.toNextSibling(XmlCursor.ELEMENT)) {
                popPosition();
                return false;
            }
            if (localName.equals(c.getNodeLocalName()) &&
                    namespaceUri.equals(c.getNodeNamespaceUri()))
            {
                return true;
            }
        }
        /* NOTREACHED */
    }

    public String getAttributeValue(String name) {
        pushPosition();
        try {
            for (;;) {
                if (!c.toNextAttribute()) {
                    return null;
                }
                if (name.equals(c.getNodeName()))
                    return c.getNodeValue();
            }
        } finally {
            popPosition();
        }
    }

    public String getAttributeValue(String localName, String namespaceUri) {
        pushPosition();
        try {
            for (;;) {
                if (!c.toNextAttribute()) {
                    return null;
                }
                if (localName.equals(c.getNodeLocalName()) && namespaceUri.equals(c.getNodeNamespaceUri()))
                    return c.getNodeValue();
            }
        } finally {
            popPosition();
        }
    }

    public String getAttributeValue(String localName, String[] namespaceUris) {
        pushPosition();
        try {
            for (;;) {
                if (!c.toNextAttribute()) {
                    return null;
                }
                if (localName.equals(c.getNodeLocalName()) &&
                        ArrayUtils.contains(namespaceUris, c.getNodeNamespaceUri()))
                    return c.getNodeValue();
            }
        } finally {
            popPosition();
        }
    }

    public String getLocalName() {
        return c.getNodeLocalName();
    }

    public String getNamespaceUri() {
        return c.getNodeNamespaceUri();
    }

    public String getPrefix() {
        return c.getNodePrefix();
    }

    public String getTextValue() {
        pushPosition();
        try {
            if (!c.toFirstChild(XmlCursor.TEXT))
                return "";
            StringBuffer sb = new StringBuffer();

            do {
                sb.append(c.getNodeValue());
            } while (c.toNextSibling(XmlCursor.TEXT));

            return sb.toString().trim();
        } finally {
            popPosition();
        }
    }

    public void write(OutputStream outputStream) throws IOException {
        OutputFormat of = new OutputFormat();
        of.setOmitXmlDeclaration(true);
        of.setIndent(false);
        of.setEncoding(Encoding.UTF8);
        XmlResult result = new XmlResult(outputStream);
        c.writeTo(of, result);
    }

    public Element asDomElement(Document factory) {
        DomOutput output = new DomOutput(factory);
        try {
            c.writeTo(output);
        } catch (IOException e) {
            throw new RuntimeException(e); // shouldn't be possible
        }
        return (Element)output.getResultRoot();
    }

    public XpathResult getXpathResult(CompiledXpath compiledXpath) throws XPathExpressionException {
        if (compiledXpath == CompiledXpath.ALWAYS_TRUE)
            return XpathResult.RESULT_TRUE;
        if (compiledXpath == CompiledXpath.ALWAYS_FALSE)
            return XpathResult.RESULT_FALSE;

        if (compiledXpath instanceof TarariCompiledXpath) {
            TarariCompiledXpath tarariCompiledXpath = (TarariCompiledXpath)compiledXpath;
            return tarariCompiledXpath.getXpathResult(this);
        }

        // Someone passed a non-Tarari CompiledXpath to a Tarari cursor.  It shouldn't be possible to even
        // construct a non-Tarari CompiledXpath if you are able to construct Tarari cursors.
        throw new XPathExpressionException("Non-Tarari CompiledXpath passed to TarariElementCursor");
    }
}
