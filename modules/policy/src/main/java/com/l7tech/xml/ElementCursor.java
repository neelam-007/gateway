/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.xml;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.util.ArrayUtils;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.util.TooManyChildElementsException;
import com.l7tech.xml.tarari.TarariMessageContext;
import com.l7tech.xml.xpath.CompiledXpath;
import com.l7tech.xml.xpath.XpathResult;
import com.l7tech.xml.xpath.XpathVariableFinder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Represents an element position (or the root Document node) in an abstract XML document which
 * may use DOM or Tarari XmlCursor as its underlying implementation.
 * This is the central abstraction in the Layer 7 XML API.
 * <p/>
 * Cursors can be cheaply copied; however, to keep down the amount of garbage produced while processing a request,
 * it is encouraged to avoid creating new cursors whenever existing ones can be reused instead.
 * <p/>
 * This is very closely based on the Tarari XmlCursor interface.
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
     * Position cursor at ROOT (the node representing the Document, right above the Document element).  Always succeeds.
     */
    public abstract void moveToRoot();

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
    public boolean moveToOnlyOneChildElement() throws TooManyChildElementsException {
        boolean b = moveToFirstChildElement();
        if (!b)
            return false;

        // It worked -- now just make sure it's the only one
        pushPosition();
        if (moveToNextSiblingElement()) {
            moveToParentElement();
            String localName = getLocalName();
            String nsuri = getNamespaceUri();
            popPosition(true); // Leave it pointing at parent
            throw new TooManyChildElementsException(nsuri, localName);
        }
        popPosition();
        return true;
    }

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
     * <p/>
     * The advantage of using this method over just calling getAttributeValue().equals() is that implementations
     * may be able to accelerate the comparison (especially failed comparisons) and avoid having to convert
     * the attribute's current value into a Java String (which may be slow and produce garbage).
     *
     * @param attrName   the name of the local attribute to match.  Not a qname -- this is only for local attributes.
     * @param values     matrix of possible attribute values to match.  Must not be null and must not contain nulls.
     * @return index of matching row in values, or -1 if no matching attribute value.
     */
    public int matchAttributeValue(String attrName, String[][] values) {
        String val = getAttributeValue(attrName);
        for (int i = 0; i < values.length; i++) {
            String[] row = values[i];
            for (String wantVal : row) {
                if (val.equals(wantVal))
                    return i;
            }
        }
        return -1;
    }

    /**
     * Check if the current element contains any children other than attributes and child elements.
     *
     * @param ignoreWhitespace if ture, text nodes containing only whitespace will be ignored.
     * @param ignoreComments if true, comment nodes will be ignored.
     * @return true if any mixed mode content was detected.
     */
    public abstract boolean containsMixedModeContent(boolean ignoreWhitespace, boolean ignoreComments);

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
     * Serialize the current element out to the specified output stream in UTF-8 encoding.
     *
     * @param outputStream the output stream.  Must not be null.
     * @throws IOException if there is a problem writing to the output stream
     */
    public abstract void write(OutputStream outputStream) throws IOException;

    /**
     * Serialize the current element as a String.
     *
     * @throws IOException if there is a problem serializing
     * @return the current element as a String, from opening angle bracket of open tag up to and including
     *         closing angle bracket of close tag.
     */
    public abstract String asString() throws IOException;

    /**
     * Build a DOM tree representation of the current element.
     * <p/>
     * Keep in mind that this is expensive and produces lots of garbage.
     *
     * @param factory the DOM Document to use to build the new DOM tree.  Must not be null.
     * @return a DOM Element view of the current element.  Never null.
     */
    public abstract Element asDomElement(Document factory);

    /**
     * Get a DOM tree representation of the current element, using or reusing any that's available.
     * <p/>
     * Unless this cursor already happens to be based on a DOM tree, this is expensive and creates a lot of garbage.
     *
     * @return a DOM Element view of the current element, which must not be modified in any way.  Never null.
     */
    public Element asDomElement() {
        return asDomElement(XmlUtil.createEmptyDocument());
    }

    /**
     * Get the TarariMessageContext representation of the message to which this cursor belongs, if this
     * is a Tarari-based ElementCursor.
     *
     * @return the TarariMessageContext for this cursor, or null if this is not a Tarari-based cursor.
     */
    public TarariMessageContext getTarariMessageContext() {
        return null;
    }

    /**
     * Run the specified already-compiled XPath expression against this cursor at its current position and return
     * a new XpathResult instance.
     *
     * @param compiledXpath the compiled XPath to run against this cursor.  Must not be null.
     * @param variableFinder a callback to invoke to determine the values of any XPath variables, or null to disable XPath variables.
     * @param requireCursor true if any nodeset result must be navigable using an ElementCursor. Disables fastxpath for this result.
     * @return the results of running this XPath against this cursor at its current position.  Never null.
     * @throws XPathExpressionException if the match failed and no result could be produced.
     */
    public abstract XpathResult getXpathResult(CompiledXpath compiledXpath, XpathVariableFinder variableFinder, boolean requireCursor) throws XPathExpressionException;

    /**
     * Run the specified already-compiled XPath expression against this cursor at its current position and return
     * a new XpathResult instance. Equivalent to calling {@link #getXpathResult(com.l7tech.xml.xpath.CompiledXpath, XpathVariableFinder, boolean)} with
     * the second argument <code>null</code> and 
     * the third argument <code>false</code>.
     *
     * @param compiledXpath the compiled XPath to run against this cursor.  Must not be null.
     * @return the results of running this XPath against this cursor at its current position.  Never null.
     * @throws XPathExpressionException if the match failed and no result could be produced.
     */
    public XpathResult getXpathResult(CompiledXpath compiledXpath) throws XPathExpressionException {
        return getXpathResult(compiledXpath, null, false);
    }

    /**
     * Convenience method that runs the specified already-compiled XPath expression against this cursor
     * at its current position and return true if the result was a match.
     * A result is considered a match if it's a non-empty nodeset, a true
     * boolean, or a string or number value.
     * <p/>
     * With this method, there is no way to get any more detailed information about the result
     * than whether or not it matched.  If more details are required use 
     * {@link #getXpathResult(CompiledXpath) getXpathResult} instead.
     *
     * @param compiledXpath the xpath to try to match.  Must not be null.
     * @return true if this xpath matches this cursor at its current position.
     * @throws XPathExpressionException if the match failed and no result could be produced.
     */
    public boolean matches(CompiledXpath compiledXpath) throws XPathExpressionException {
        if (compiledXpath == null) throw new IllegalArgumentException("compiledXpath must be provided");
        XpathResult result = getXpathResult(compiledXpath, null, false);
        return result != null && result.matches();
    }

    /**
     * Visit all immediate child elements of the current element.
     *
     * @param visitor a Visitor whose {@link Visitor#visit} method will immediately be invoked on every immediate
     *                child element of the current element.  Required.
     * @return the number of immediate child elements visited.
     * @throws InvalidDocumentFormatException if a visitor threw this exception.
     */
    public int visitChildElements(Visitor visitor) throws InvalidDocumentFormatException {
        pushPosition();
        try {
            if (!moveToFirstChildElement()) return 0;
            int num = 0;

            do {
                pushPosition();
                try {
                    visitor.visit(this);
                } finally {
                    popPosition(false);
                }
                num++;
            } while (moveToNextSiblingElement());

            return num;
        } finally {
            popPosition(false);
        }
    }

    /**
     * Canonicalize this element and all child data per Exclusive XML Canonicalization Version 1.0.  This is
     * needed for XML signatures.
     *
     * @param inclusiveNamespacePrefixes  namespace prefixes whose declarations should be included in the output even
     *                                    if these declarations would otherwise appear to be unused and hence
     *                                    unnecessary to include in the output.
     * @return bytes of a well-formed XML document consisting of just the current elment and all children, in canonical
     *         format.
     * @throws IOException if there is a problem producing the canonicalized output.
     */
    public abstract byte[] canonicalize(String[] inclusiveNamespacePrefixes) throws IOException;

    /** Interface implemented by users of {@link ElementCursor#visitChildElements}. */
    public interface Visitor {
        /**
         * Visit the location pointed to by this cursor.
         *
         * @param ec an ElementCursor pointed at some element of interest.  Never null.
         * @throws InvalidDocumentFormatException if some problem is discovered with this element that should
         *                                        cause the visiting process to halt.
         */
        void visit(ElementCursor ec) throws InvalidDocumentFormatException;
    }

}
