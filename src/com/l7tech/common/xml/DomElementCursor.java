/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.xml;

import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.util.ArrayUtils;
import org.w3c.dom.*;

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
    private Element cur;
    private LinkedList stack = null;

    /**
     * Create a new DomElementCursor that provides read-only access to the specified Document.  Caller must
     * ensure that nobody else modifies the Document during the lifetime of this DomElementCursor instance.
     *
     * @param doc the Document to wrap.  Must not be null.
     */
    public DomElementCursor(Document doc) {
        if (doc == null) throw new IllegalArgumentException("doc must be non-null");
        this.doc = doc;
        this.cur = doc.getDocumentElement();
    }

    private DomElementCursor(Document doc, Element cur, LinkedList stack) {
        this.doc = doc;
        this.cur = cur;
        this.stack = stack;
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
        LinkedList s = stack != null && !stack.isEmpty() ? new LinkedList(stack) : null;
        return new DomElementCursor(doc, cur, s);
    }

    public void pushPosition() {
        if (stack == null) stack = new LinkedList();
        stack.addLast(cur);
    }

    public void popPosition() throws IllegalStateException {
        if (stack == null || stack.isEmpty()) throw new IllegalStateException("No saved position");
        cur = (Element)stack.removeLast();
    }

    public void popPosition(boolean discard) throws IllegalStateException {
        if (stack == null || stack.isEmpty()) throw new IllegalStateException("No saved position");
        if (discard)
            stack.removeLast();
        else
            cur = (Element)stack.removeFirst();
    }

    public void moveToDocumentElement() {
        cur = doc.getDocumentElement();
    }

    public boolean moveToParentElement() {
        Node p = cur.getParentNode();
        if (p == null || p == cur || p == doc)
            return false;
        assert p.getNodeType() == Node.ELEMENT_NODE;
        cur = (Element)p;
        return true;
    }

    public boolean moveToOnlyOneChildElement() throws TooManyChildElementsException {
        Element kid = XmlUtil.findOnlyOneChildElement(cur);
        if (kid == null) return false;
        cur = kid;
        return true;
    }

    public boolean moveToFirstChildElement() {
        Element kid = XmlUtil.findFirstChildElement(cur);
        if (kid == null) return false;
        cur = kid;
        return true;
    }

    public boolean moveToNextSiblingElement() {
        for (Node n = cur.getNextSibling(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                cur = (Element)n;
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
                cur = (Element)n;
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
                cur = (Element)n;
                return true;
            }
        }
        return false;
    }

    public String getAttributeValue(String name) {
        return cur.getAttribute(name);
    }

    public String getAttributeValue(String localName, String namespaceUri) {
        return cur.getAttributeNS(namespaceUri, localName);
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
        return XmlUtil.getTextValue(cur);
    }

    public void write(OutputStream outputStream) throws IOException {
        XmlUtil.nodeToOutputStream(cur, outputStream, "UTF-8");
    }

    public Element asDomElement(Document factory) {
        return (Element)factory.importNode(cur, true);
    }

    public Element asDomElement() {
        return cur;
    }
}
