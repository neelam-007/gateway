/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.xml;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.util.ArrayUtils;
import com.l7tech.util.PoolByteArrayOutputStream;
import com.l7tech.util.Charsets;
import com.l7tech.util.DomUtils;
import com.l7tech.xml.xpath.CompiledXpath;
import com.l7tech.xml.xpath.DomCompiledXpath;
import com.l7tech.xml.xpath.XpathResult;
import com.l7tech.xml.xpath.XpathVariableFinder;
import org.w3c.dom.*;

import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;

/**
 * An implementation of {@link ElementCursor} that uses a DOM tree as its underlying model.
 * It is the users responsibility to ensure that the document is not modified by anyone else while at least one
 * DomElementCursor is still using it.
 */
public class DomElementCursor extends ElementCursor {
    private final Document doc;
    private Node cur;
    private LinkedList<Node> stack = null;

    /**
     * Create a new DomElementCursor that provides read-only access to the specified Document.  Caller must
     * ensure that nobody else modifies the Document during the lifetime of this DomElementCursor instance.
     *
     * Moves to the root of the document.
     *
     * @param n the Document to wrap.  Must not be null.
     */
    public DomElementCursor(Node n) {
        this(n, true);
    }

    /**
     * Create a new DomElementCursor that provides read-only access to the specified Document.  Caller must
     * ensure that nobody else modifies the Document during the lifetime of this DomElementCursor instance.
     *
     * @param n                     the Document to wrap.  Must not be null.
     * @param moveToDocumentElement true if this cursor should be positioned at the document element; false if it should be left at the specified node.
     */
    public DomElementCursor(Node n, boolean moveToDocumentElement) {
        if (n == null) throw new IllegalArgumentException("node must be non-null");
        if (n.getNodeType() != Node.ELEMENT_NODE && n.getNodeType() != Node.DOCUMENT_NODE)
            throw new IllegalArgumentException("node must be an Element or Document");

        doc = n.getNodeType() == Node.DOCUMENT_NODE ? (Document)n : n.getOwnerDocument();
        cur = moveToDocumentElement ? doc.getDocumentElement() : n;
    }


    private DomElementCursor(Document doc, Node cur, LinkedList<Node> stack) {
        this.doc = doc;
        this.cur = cur;
        this.stack = stack;
        if (cur.getNodeType() != Node.ELEMENT_NODE && cur.getNodeType() != Node.DOCUMENT_NODE)
            throw new IllegalArgumentException("current node must be an element or the root node");
    }

    /**
     * Get the Document that this ElementCursor is attached to.  Caller must not modify the returned document
     * in any way -- doing so will invalidate the cursor.
     *
     * @return the attached Document.  Never null.  This document must not be modified in any way.
     */
    public Document getDocument() {
        return doc;
    }

    public ElementCursor duplicate() {
        // Don't bother copying an empty stack
        LinkedList<Node> s = stack != null && !stack.isEmpty() ? new LinkedList<Node>(stack) : null;
        return new DomElementCursor(doc, cur, s);
    }

    public void pushPosition() {
        if (stack == null) stack = new LinkedList<Node>();
        stack.addLast(cur);
    }

    public void popPosition() throws IllegalStateException {
        if (stack == null || stack.isEmpty()) throw new IllegalStateException("No saved position");
        cur = stack.removeLast();
    }

    public void popPosition(boolean discard) throws IllegalStateException {
        if (stack == null || stack.isEmpty()) throw new IllegalStateException("No saved position");
        if (discard)
            stack.removeLast();
        else
            cur = stack.removeLast();
    }

    public void moveToDocumentElement() {
        cur = doc.getDocumentElement();
    }

    public void moveToRoot() {
        cur = doc;
    }

    public boolean moveToParentElement() {
        Node p = cur.getParentNode();
        if (p == null || p == cur || p == doc)
            return false;
        assert p.getNodeType() == Node.ELEMENT_NODE;
        cur = p;
        return true;
    }

    public boolean moveToFirstChildElement() {
        Element kid = DomUtils.findFirstChildElement(asDomElement());
        if (kid == null) return false;
        cur = kid;
        return true;
    }

    public boolean moveToNextSiblingElement() {
        for (Node n = cur.getNextSibling(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                cur = n;
                return true;
            }
        }
        return false;
    }

    public boolean moveToNextSiblingElement(String localName, String[] namespaceUris) {
        for (Node n = cur.getNextSibling(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() == Node.ELEMENT_NODE &&
                    localName.equals(n.getLocalName()) &&
                    ArrayUtils.contains(namespaceUris, n.getNamespaceURI()))
            {
                cur = n;
                return true;
            }
        }
        return false;
    }

    public boolean moveToNextSiblingElement(String localName, String namespaceUri) {
        for (Node n = cur.getNextSibling(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() == Node.ELEMENT_NODE &&
                    localName.equals(n.getLocalName()) &&
                    namespaceUri.equals(n.getNamespaceURI()))
            {
                cur = n;
                return true;
            }
        }
        return false;
    }

    public String getAttributeValue(String name) {
        return asDomElement().getAttribute(name);
    }

    public String getAttributeValue(String localName, String namespaceUri) {
        return asDomElement().getAttributeNS(namespaceUri, localName);
    }

    public String getAttributeValue(String localName, String[] namespaceUris) {
        NamedNodeMap attrs = cur.getAttributes();
        int len = attrs.getLength();
        for (int i = 0; i < len; ++i) {
            Attr attr = (Attr)attrs.item(i);
            if (localName.equals(attr.getLocalName()) &&
                    ArrayUtils.contains(namespaceUris, attr.getNamespaceURI()))
                return attr.getNodeValue();
        }
        return null;
    }

    public boolean containsMixedModeContent(boolean ignoreWhitespace, boolean ignoreComments) {
        NodeList nodes = cur.getChildNodes();
        SCAN: for (int i = 0; i < nodes.getLength(); ++i) {
            Node node = nodes.item(i);
            switch (node.getNodeType()) {
                case Node.ATTRIBUTE_NODE:
                case Node.ELEMENT_NODE:
                    continue SCAN;

                case Node.COMMENT_NODE:
                    if (ignoreComments)
                        continue SCAN;
                    return true;

                case Node.CDATA_SECTION_NODE:
                    return true;

                case Node.TEXT_NODE:
                    if (ignoreWhitespace) {
                        String text = node.getTextContent();
                        if (text != null && text.trim().length() < 1)
                            continue SCAN;
                    }
                    return true;

                default:
                    return true;
            }
        }

        return false;
    }

    public String getLocalName() {
        return cur.getLocalName();
    }

    public String getNamespaceUri() {
        return cur.getNamespaceURI();
    }

    public String getPrefix() {
        return cur.getPrefix();
    }

    public String getTextValue() {
        return DomUtils.getTextValue(asDomElement());
    }

    public void write(OutputStream outputStream) throws IOException {
        XmlUtil.nodeToOutputStream(cur, outputStream, "UTF-8");
    }

    public String asString() throws IOException {
        PoolByteArrayOutputStream baos = new PoolByteArrayOutputStream(4096);
        XmlUtil.canonicalize(cur, baos);
        return baos.toString(Charsets.UTF8);
    }

    public Element asDomElement(Document factory) {
        return toDomElement(factory.importNode(cur, true));
    }

    public Element asDomElement() {
        return toDomElement(this.cur);
    }

    protected Element toDomElement(Node cur) {
        if (cur instanceof Element)
            return (Element)cur;
        if (cur instanceof Document)
            return ((Document)cur).getDocumentElement();
        throw new IllegalStateException("node not an Element or Document");
    }

    /**
     * @return the current Node of this DomElementCursor.  Might be Element or Document.  Never null. 
     */
    public Node asDomNode() {
        return cur;
    }

    public XpathResult getXpathResult(CompiledXpath compiledXpath, XpathVariableFinder variableFinder, boolean unused) throws XPathExpressionException {
        if (compiledXpath == CompiledXpath.ALWAYS_TRUE)
            return XpathResult.RESULT_TRUE;
        if (compiledXpath == CompiledXpath.ALWAYS_FALSE)
            return XpathResult.RESULT_FALSE;

        if (compiledXpath instanceof DomCompiledXpath) {
            DomCompiledXpath domCompiledXpath = (DomCompiledXpath)compiledXpath;
            return domCompiledXpath.getXpathResult(this, variableFinder);
        }

        // This can't happen -- currently there are only two impls, TarariCompiledXpath and DomCompiledXpath,
        // and TarariCompiledXpath extends DomCompiledXpath.
        throw new IllegalArgumentException("Unsupported CompiledXpath of type " + compiledXpath.getClass().getName());
    }

    public byte[] canonicalize(String[] inclusiveNamespacePrefixes) throws IOException {
        PoolByteArrayOutputStream baos = new PoolByteArrayOutputStream();
        try {
            XmlUtil.canonicalize(cur, baos);
            return baos.toByteArray();
        } finally {
            baos.close();
        }
    }
}
