/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.xml;

import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.util.ArrayUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Represents an element position in an abstract XML document which may use DOM or Tarari XmlCursor as its
 * underlying implementation.  This is the central abstraction in the Layer 7 XML API.
 * <p/>
 * Cursors can be cheaply copied, however, it is strongly encouraged to avoid creating new cursors when existing
 * ones can be reused instead.
 */
public abstract class ElementCursor {
    /**
     * Return a new cursor positioned at the same location as this cursor.
     * <p/>
     * Copying a cursor is relatively cheap, but does produce extra garbage.
     *
     * @return a copy of the current cursor.  Never null.
     */
    public abstract ElementCursor duplicate();

    // -- Cursor movement --

    /** Save the current cursor position on an internal stack. */
    public abstract void pushPosition();

    /**
     * Restore the most recently pushed cursor position.
     *
     * @throws IllegalStateException  if there is no position currently saved
     */
    public abstract void popPosition() throws IllegalStateException;

    /**
     * Restore the most recently pushed cursor position, or just forget it without changing the current position.
     *
     * @param discard  if true, throw away the last saved position without changing the current position
     * @throws IllegalStateException  if there is no position currently saved
     */
    public abstract void popPosition(boolean discard) throws IllegalStateException;

    /**
     * Position cursor at document element.  Always succeeds.
     */
    public abstract void moveToDocumentElement();

    /**
     * Position cursor at parent element of current element, unless current element is the document element.
     *
     * @return true if the cursor was moved.
     */
    public abstract boolean moveToParentElement();

    /**
     * Position cursor at first child element of current element, if it has one.  Throws if current element has
     * more than one child element.
     *
     * @return true if the cursor was moved to a child element; false if there was no child element.
     * @throws TooManyChildElementsException if current element has more than one child element.  Cursor is
     *                                       left unmoved, pointing at the original current element.
     */
    public abstract boolean moveToOnlyOneChildElement() throws TooManyChildElementsException;

    /**
     * Position cursor at first child element of current element, if it has one.
     *
     * @return true if the cursor was moved to a child element; false if there was no child element.
     */
    public abstract boolean moveToFirstChildElement();

    /**
     * Position cursor at first child element that has the specified localname and namespace URI.
     *
     * @param localName       the local name to match.  Must not be null or empty.
     * @param namespaceUri    the namespace URI to match.  Must not be null.
     * @return true if cursor was moved to the first matching first child; false if there was no matching child.
     */
    public boolean moveToFirstChildElement(String localName, String namespaceUri) {
        pushPosition();

        if (!moveToFirstChildElement()) {
            popPosition();
            return false;
        }

        if (localName.equals(getLocalName()) && namespaceUri.equals(getNamespaceUri())) {
            popPosition(true);
            return true;
        }

        if (!moveToNextSiblingElement(localName, namespaceUri)) {
            popPosition();
            return false;
        }

        popPosition(true);
        return true;
    }

    /**
     * Position cursor at first child element that has the specified localname and namespace URI.
     *
     * @param localName       the local name to match.  Must not be null or empty.
     * @param namespaceUris   the list of namespace URIs to match.  Must not be null or empty, and may not contain nulls.
     * @return true if cursor was moved to the first matching first child; false if there was no matching child.
     */
    public boolean moveToFirstChildElement(String localName, String[] namespaceUris) {
        pushPosition();

        if (!moveToFirstChildElement()) {
            popPosition();
            return false;
        }

        if (localName.equals(getLocalName()) && ArrayUtils.contains(namespaceUris, getNamespaceUri())) {
            popPosition(true);
            return true;
        }

        if (!moveToNextSiblingElement(localName, namespaceUris)) {
            popPosition();
            return false;
        }

        popPosition(true);
        return true;
    }

    /**
     * Position the cursor at next sibling element, if there is one.
     *
     * @return true if cursor was moved to a next sibling; false if there was no next sibling.
     */
    public abstract boolean moveToNextSiblingElement();

    /**
     * Position cursor at next sibling element that matches the specified name, if one is found.
     *
     * @param localName       the local name to match.  Must not be null or empty.
     * @param namespaceUris   the list of namespace URIs to match.  Must not be null or empty, and may not contain nulls.
     * @return true if cursor was moved to a matching next sibling; false if there was no matching next sibling.
     */
    public abstract boolean moveToNextSiblingElement(String localName, String[] namespaceUris);

    /**
     * Position cursor at next sibling element that matches the specified name, if one is found.
     *
     * @param localName       the local name to match.  Must not be null or empty.
     * @param namespaceUri    the namespace URI to match.  Must not be null.
     * @return true if cursor was moved to a matching next sibling; false if there was no matching next sibling.
     */
    public abstract boolean moveToNextSiblingElement(String localName, String namespaceUri);

    // -- Accessors --

    /**
     * Obtain the value of an attribute of the current element.  This will only find local attributes which
     * are not in any namespace.
     *
     * @param name   the name of the attribute.  Must not be null or empty.
     * @return the attribute value, which may be empty, or null if no matching attribute was found.
     */
    public abstract String getAttributeValue(String name);

    /**
     * Obtain the value of an attribute of the current element.
     *
     * @param localName       the local name to match.  Must not be null or empty.
     * @param namespaceUri   the namespace URI to match.  Must not be null or empty.
     * @return the attribute value, which may be empty, or null if no matching attribute was found.
     */
    public abstract String getAttributeValue(String localName, String namespaceUri);

    /**
     * Obtain the value of an attribute of the current element, finding it by a list of namespace URIs.
     * This can be on the slow side -- before using this method, make sure {@link #matchAttributeValue} won't
     * meet your needs.
     *
     * @param localName       the local name to match.  Must not be null or empty.
     * @param namespaceUris   the list of namespace URIs to match.  Must not be null or empty, and may not contain nulls.
     * @return the attribute value, which may be empty, or null if no matching attribute was found.
     */
    public abstract String getAttributeValue(String localName, String[] namespaceUris);

    /**
     * Check if the current element has a local attribute with the given name and one of the specified
     * values (which must match exactly; qname matching of attribute values is not supported by this method).
     * <p/>
     * Example:
     * <pre>
     *    int result = matchAttributeValue("ValueType", new String[][] {
     *         new String[] { SoapUtil.VALUETYPE_X509, SoapUtil.VALUETYPE_X509_2 }, // synonyms for X.509
     *         new String[] { SoapUtil.VALUETYPE_SKI },                             // synonyms for SKI
     *    });
     * </pre>
     *
     * @param attrName   the name of the local attribute to match.  Not a qname -- this is only for local attributes.
     * @param values     matrix of possible attribute values to match.  Must not be null and must not contain nulls.
     * @return index of matching row in values, or -1 if no matching attribute value.
     */
    public int matchAttributeValue(String attrName, String[][] values) {
        String val = getAttributeValue(attrName);
        for (int i = 0; i < values.length; i++) {
            String[] row = values[i];
            for (int j = 0; j < row.length; j++) {
                String wantVal = row[j];
                if (val.equals(wantVal))
                    return i;
            }
        }
        return -1;
    }

    /**
     * Get the local name of the current element.
     *
     * @return the local name.  Never null.
     */
    public abstract String getLocalName();

    /**
     * Get the namespace URI of the current element.
     *
     * @return the namespace URI.  May be empty but never null.
     */
    public abstract String getNamespaceUri();

    /**
     * Get the prefix of the current element.
     *
     * @return the element prefix, or null if the element has no prefix.
     */
    public abstract String getPrefix();

    /**
     * Gets the child text node value for an element.
     *
     * @return a String consisting of all text nodes glued together and then trimmed.  May be empty but never null.
     */
    public abstract String getTextValue();

    // -- Export --

    /**
     * Serialize the current element out to the specified output stream.
     *
     * @param outputStream the output stream.  Must not be null.
     * @throws IOException if there is a problem writing to the output stream
     */
    public abstract void write(OutputStream outputStream) throws IOException;

    /**
     * Build a DOM tree representation of the current element.
     * <p/>
     * Keep in mind that this is expensive and produces lots of garbage.
     *
     * @param factory the DOM Document to use to build the new
     * @return a DOM Element view of the current element.  Never null.
     */
    public abstract Element asDomElement(Document factory);

    /**
     * Get a DOM tree representation of the current element, using or reusing any that's avialable.
     * <p/>
     * This is still expensive, and still creates a lot of garbage.
     *
     * @return a DOM Element view of the current element, which must not be modified in any way.  Never null.
     */
    public Element asDomElement() {
        return asDomElement(XmlUtil.createEmptyDocument());
    }
}
