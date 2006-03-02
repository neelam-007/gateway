/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.xml.tarari;

import com.l7tech.common.util.ArrayUtils;
import com.l7tech.common.xml.ElementCursor;
import com.l7tech.common.xml.TooManyChildElementsException;
import com.tarari.io.Encoding;
import com.tarari.xml.XmlResult;
import com.tarari.xml.cursor.XmlCursor;
import com.tarari.xml.output.DomOutput;
import com.tarari.xml.output.OutputFormat;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.IOException;
import java.io.OutputStream;

/**
 * An {@link ElementCursor} implementation that wraps a Tarari XmlCursor and provides features tuned for the
 * read-only use of a WSS processor, but not so much for generic XML processing.
 */
public class TarariElementCursor extends ElementCursor {
    private final XmlCursor c;

    public TarariElementCursor(XmlCursor xmlCursor) {
        if (xmlCursor == null) throw new IllegalArgumentException("A RaxCursor must be provided");
        this.c = xmlCursor;
        c.toDocumentElement(); // Make sure it's pointing at an element
        assert c.getNodeType() == XmlCursor.ELEMENT;
    }

    public ElementCursor duplicate() {
        assert c.getNodeType() == XmlCursor.ELEMENT;
        return new TarariElementCursor(c.duplicate());
    }

    public void pushPosition() {
        assert c.getNodeType() == XmlCursor.ELEMENT;
        c.pushPosition();
    }

    public void popPosition() throws IllegalStateException {
        assert c.getNodeType() == XmlCursor.ELEMENT;
        c.popPosition();
    }

    public void popPosition(boolean discard) throws IllegalStateException {
        assert c.getNodeType() == XmlCursor.ELEMENT;
        c.popPosition(discard);
    }

    public void moveToDocumentElement() {
        assert c.getNodeType() == XmlCursor.ELEMENT;
        c.toDocumentElement();
    }

    public boolean moveToParentElement() {
        assert c.getNodeType() == XmlCursor.ELEMENT;
        return c.toParent();
    }

    public boolean moveToOnlyOneChildElement() throws TooManyChildElementsException {
        assert c.getNodeType() == XmlCursor.ELEMENT;
        boolean b = c.toFirstChild(XmlCursor.ELEMENT);
        if (!b)
            return false;

        // It worked -- now just make sure it's the only one
        c.pushPosition();
        if (c.toNextSibling(XmlCursor.ELEMENT)) {
            c.toParent();
            String localName = c.getNodeLocalName();
            String nsuri = c.getNodeNamespaceUri();
            c.popPosition(true); // Leave it pointing at parent
            throw new TooManyChildElementsException(nsuri, localName);
        }
        c.popPosition();
        return true;
    }

    public boolean moveToFirstChildElement() {
        assert c.getNodeType() == XmlCursor.ELEMENT;
        return c.toFirstChild(XmlCursor.ELEMENT);
    }

    public boolean moveToNextSiblingElement() {
        assert c.getNodeType() == XmlCursor.ELEMENT;
        return c.toNextSibling(XmlCursor.ELEMENT);
    }

    public boolean moveToNextSiblingElement(String localName, String[] namespaceUris) {
        assert c.getNodeType() == XmlCursor.ELEMENT;
        c.pushPosition();
        for (;;) {
            if (!c.toNextSibling(XmlCursor.ELEMENT)) {
                c.popPosition();
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
        assert c.getNodeType() == XmlCursor.ELEMENT;
        c.pushPosition();
        for (;;) {
            if (!c.toNextSibling(XmlCursor.ELEMENT)) {
                c.popPosition();
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
        assert c.getNodeType() == XmlCursor.ELEMENT;
        c.pushPosition();
        try {
            for (;;) {
                if (!c.toNextAttribute()) {
                    return null;
                }
                if (name.equals(c.getNodeName()))
                    return c.getNodeValue();
            }
        } finally {
            c.popPosition();
        }
    }

    public String getAttributeValue(String localName, String namespaceUri) {
        assert c.getNodeType() == XmlCursor.ELEMENT;
        c.pushPosition();
        try {
            for (;;) {
                if (!c.toNextAttribute()) {
                    return null;
                }
                if (localName.equals(c.getNodeLocalName()) && namespaceUri.equals(c.getNodeNamespaceUri()))
                    return c.getNodeValue();
            }
        } finally {
            c.popPosition();
        }
    }

    public String getAttributeValue(String localName, String[] namespaceUris) {
        assert c.getNodeType() == XmlCursor.ELEMENT;
        c.pushPosition();
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
            c.popPosition();
        }
    }

    public String getLocalName() {
        assert c.getNodeType() == XmlCursor.ELEMENT;
        return c.getNodeLocalName();
    }

    public String getNamespaceUri() {
        assert c.getNodeType() == XmlCursor.ELEMENT;
        return c.getNodeNamespaceUri();
    }

    public String getPrefix() {
        assert c.getNodeType() == XmlCursor.ELEMENT;
        return c.getNodePrefix();
    }

    public String getTextValue() {
        assert c.getNodeType() == XmlCursor.ELEMENT;
        c.pushPosition();
        try {
            if (!c.toFirstChild(XmlCursor.TEXT))
                return "";
            StringBuffer sb = new StringBuffer();

            do {
                sb.append(c.getNodeValue());
            } while (c.toNextSibling(XmlCursor.TEXT));

            return sb.toString().trim();
        } finally {
            c.popPosition();
        }
    }

    public void write(OutputStream outputStream) throws IOException {
        assert c.getNodeType() == XmlCursor.ELEMENT;

        OutputFormat of = new OutputFormat();
        of.setOmitXmlDeclaration(true);
        of.setIndent(false);
        of.setEncoding(Encoding.UTF8);
        XmlResult result = new XmlResult(outputStream);
        c.writeTo(of, result);
    }

    public Element asDomElement(Document factory) {
        assert c.getNodeType() == XmlCursor.ELEMENT;
        DomOutput output = new DomOutput(factory);
        try {
            c.writeTo(output);
        } catch (IOException e) {
            throw new RuntimeException(e); // shouldn't be possible
        }
        return (Element)output.getResultRoot();
    }
}
