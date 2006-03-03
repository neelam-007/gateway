/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.xml;

import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.util.ArrayUtils;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.xml.xpath.XpathResult;
import com.l7tech.common.xml.xpath.CompiledXpath;
import com.l7tech.common.xml.xpath.DomCompiledXpath;
import org.w3c.dom.*;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An implementation of {@link ElementCursor} that uses a DOM tree as its underlying model.
 * It is the users responsibility to ensure that the document is not modified by anyone else while at least one
 * DomElementCursor is still using it.
 */
public class DomElementCursor extends ElementCursor {
    private static final Logger logger = Logger.getLogger(DomElementCursor.class.getName());
    private final Document doc;
    private Node cur;
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

    private DomElementCursor(Document doc, Node cur, LinkedList stack) {
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
        Element kid = XmlUtil.findFirstChildElement(asDomElement());
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
        return XmlUtil.getTextValue(asDomElement());
    }

    public void write(OutputStream outputStream) throws IOException {
        XmlUtil.nodeToOutputStream(cur, outputStream, "UTF-8");
    }

    public Element asDomElement(Document factory) {
        return (Element)factory.importNode(cur, true);
    }

    public Element asDomElement() {
        if (cur instanceof Element)
            return (Element)cur;
        if (cur instanceof Document)
            return ((Document)cur).getDocumentElement();
        throw new IllegalStateException("current node not an Element or Document");
    }

    /**
     * @return the current Node of this DomElementCursor.  Might be Element or Document.  Never null. 
     */
    public Object asDomNode() {
        return cur;
    }

    public XpathResult getXpathResult(CompiledXpath compiledXpath) {
        if (compiledXpath == CompiledXpath.ALWAYS_TRUE)
            return XpathResult.RESULT_TRUE;
        if (compiledXpath == CompiledXpath.ALWAYS_FALSE)
            return XpathResult.RESULT_FALSE;

        if (compiledXpath instanceof DomCompiledXpath) {
            DomCompiledXpath domCompiledXpath = (DomCompiledXpath)compiledXpath;
            try {
                return domCompiledXpath.getXpathResult(this);
            } catch (InvalidXpathException e) {
                // Shouldn't be possible
                logger.log(Level.WARNING, "Invalid xpath expression (compiled lazily): " + ExceptionUtils.getMessage(e), e);
                return null;
            }
        }

        // This can't happen -- currently there are only two impls, TarariCompiledXpath and DomCompiledXpath,
        // and TarariCompiledXpath extends DomCompiledXpath.
        throw new IllegalArgumentException("Unsupported CompiledXpath of type " + compiledXpath.getClass().getName());
    }
}
