/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.xml.tarari;

import com.l7tech.util.PoolByteArrayOutputStream;
import com.l7tech.util.ArrayUtils;
import com.l7tech.xml.ElementCursor;
import com.l7tech.xml.xpath.CompiledXpath;
import com.l7tech.xml.xpath.XpathResult;
import com.l7tech.xml.xpath.XpathVariableFinder;
import com.tarari.io.Encoding;
import com.tarari.xml.XmlResult;
import com.tarari.xml.NodeType;
import com.tarari.xml.cursor.XmlCursor;
import com.tarari.xml.output.DomOutput;
import com.tarari.xml.output.OutputFormat;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * An {@link ElementCursor} implementation that wraps a Tarari XmlCursor and provides features tuned for the
 * read-only use of a WSS processor, but not so much for generic XML processing.
 */
class TarariElementCursor extends ElementCursor {
    private final TarariMessageContextImpl tarariMessageContext;
    private XmlCursor c;

    public TarariElementCursor(XmlCursor xmlCursor, TarariMessageContextImpl tarariMessageContext) {
        this(xmlCursor, tarariMessageContext, true);
    }

    TarariElementCursor(XmlCursor xmlCursor, TarariMessageContextImpl tmci, boolean moveToRoot) {
        if (xmlCursor == null) throw new IllegalArgumentException("An XmlCursor must be provided");
        this.c = xmlCursor;
        this.tarariMessageContext = tmci;
        if (moveToRoot) {
            c.toDocumentElement(); // Make sure it's pointing at an element
        } else {
            if (c.getNodeType() != XmlCursor.ELEMENT && c.getNodeType() != XmlCursor.ROOT)
                throw new IllegalArgumentException("Cursor was not pointed at an Element or Document");
        }

    }

    /**
     * Get the XmlCursor.  Package private.
     * @return the XmlCursor.  Never null.
     */
    XmlCursor getXmlCursor() {
        return c;
    }

    /**
     * Get the current TarariMessageContextImpl.
     * @return the TarariMessageContextImpl.  Never null.
     */
    public TarariMessageContextImpl getTarariMessageContext() {
        return tarariMessageContext;
    }

    public ElementCursor duplicate() {
        return new TarariElementCursor(c.duplicate(), tarariMessageContext, false);
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

    public boolean containsMixedModeContent(boolean ignoreWhitespace, boolean ignoreComments) {
        pushPosition();
        try {
            SCAN: for (;;) {
                if (!c.toNextSibling())
                    return false;
                int type = c.getNodeType();
                switch (type) {
                    case NodeType.ATTRIBUTE:
                    case NodeType.ELEMENT:
                    case NodeType.ELEMENT_END:
                        continue SCAN;

                    case NodeType.COMMENT:
                        if (ignoreComments)
                            continue SCAN;
                        return true;

                    case NodeType.TEXT:
                        if (ignoreWhitespace) {
                            String text = c.getNodeValue();
                            if (text != null && text.trim().length() < 1)
                                continue SCAN;
                        }
                        return true;

                    default:
                        return true;
                }
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

    private static final ThreadLocal localOutputFormat = new ThreadLocal() {
        protected Object initialValue() {
            OutputFormat of = new OutputFormat();
            of.setOmitXmlDeclaration(true);
            of.setMethod(OutputFormat.METHOD_C14N_EXCLUSIVE);
            of.setC14nWithComments(false);
            of.setIndent(false);
            of.setEncoding(Encoding.UTF8);
            return of;
        }
    };

    public void write(OutputStream outputStream) throws IOException {
        XmlResult result = new XmlResult(outputStream);
        c.writeTo((OutputFormat)localOutputFormat.get(), result);
    }

    public String asString() throws IOException {
        StringWriter sw = new StringWriter();
        XmlResult result = new XmlResult(sw);
        c.writeTo((OutputFormat)localOutputFormat.get(), result);
        return sw.toString();
    }

    public Element asDomElement(Document factory) {
        DomOutput output = new DomOutput(factory);
        try {
            c.writeTo(output);
        } catch (IOException e) {
            throw new RuntimeException(e); // shouldn't be possible
        }
        Node out = output.getResultRoot();
        return toDomElement(out);
    }

    protected Element toDomElement(Node cur) {
        if (cur instanceof Element)
            return (Element)cur;
        if (cur instanceof Document)
            return ((Document)cur).getDocumentElement();
        throw new IllegalStateException("node not an Element or Document");
    }

    public byte[] canonicalize(String[] inclusiveNamespacePrefixes) throws IOException {
        OutputFormat of = new OutputFormat();
        of.setMethod(OutputFormat.METHOD_C14N_EXCLUSIVE);
        of.setC14nWithComments(false);
        of.setOmitXmlDeclaration(true);
        if (inclusiveNamespacePrefixes != null && inclusiveNamespacePrefixes.length > 0)
            of.setC14nInclusivePrefixList(new ArrayList<String>(Arrays.asList(inclusiveNamespacePrefixes)));
        PoolByteArrayOutputStream baos = new PoolByteArrayOutputStream();
        try {
            XmlResult xr = new XmlResult(baos);
            c.writeTo(of, xr);
            return baos.toByteArray();
        } finally {
            baos.close();
        }
    }

    public XpathResult getXpathResult(CompiledXpath compiledXpath, XpathVariableFinder ignored, boolean requireCursor) throws XPathExpressionException {
        if (compiledXpath == CompiledXpath.ALWAYS_TRUE)
            return XpathResult.RESULT_TRUE;
        if (compiledXpath == CompiledXpath.ALWAYS_FALSE)
            return XpathResult.RESULT_FALSE;

        if (compiledXpath instanceof TarariCompiledXpath) {
            TarariCompiledXpath tarariCompiledXpath = (TarariCompiledXpath)compiledXpath;
            return tarariCompiledXpath.getXpathResult(this, requireCursor);
        }

        // Someone passed a non-Tarari CompiledXpath to a Tarari cursor.  This is not supposed to happen --
        // version 1.0 xpaths should always use a TarariCompiledXpath if Tarari is available, and
        // version 2.0 and up xpaths should always use a DomElementCursor.
        throw new XPathExpressionException("Non-Tarari CompiledXpath passed to TarariElementCursor");
    }

    public boolean isAtRoot() {
        return c.getNodeType() == XmlCursor.ROOT;
    }
}
