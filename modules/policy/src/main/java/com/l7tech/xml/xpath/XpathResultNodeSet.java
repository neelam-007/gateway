/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.xml.xpath;

import com.l7tech.xml.ElementCursor;

import java.util.NoSuchElementException;

/**
 * Represents a set of nodes matched by a {@link com.l7tech.xml.xpath.CompiledXpath}.
 */
public interface XpathResultNodeSet {
    /**
     * Convenience method that return true if the nodeset is empty.
     *
     * @return true if the {@link #size()} would return 0.
     */
    boolean isEmpty();

    /**
     * Get the number of items in the node set.
     *
     * @return the number of items in the nodeset.  Always nonnegative.
     */
    int size();

    /**
     * Get an iterator that will walk through the nodes in this node set.  Depending on the engine, this may be
     * considerably faster than using the random access accessors (ie, {@link #getType}, {@link #getNodePrefix}, etc).
     *
     * @return an iterator that can be used to walk through the returned nodeset.
     */
    XpathResultIterator getIterator();

    /**
     * Get the type of the node in this nodeset with the specified ordinal, represented by the short integer that
     * would have been returned by the DOM method {@link org.w3c.dom.Node#getNodeType()} if it were a DOM Node.
     *
     * @param ordinal the ordinal of the node in this result set.  Must be nonnegative and less than {@link #size()}.
     * @return a node type as defined by {@link org.w3c.dom.Node}, or -1 if the ordinal is out of range or if
     *         the expression was evaluated using Tarari fastxpath and the matching node is something other than
     *         an attribute, an element, or a text node.
     */
    int getType(int ordinal);

    /**
     * Get the prefix of the node in this nodeset with the specified ordinal, or null if it does not (or cannot)
     * have a prefix.
     * <ul>
     * <li>For elements or attributes, this will be the prefix of the qualified name, or null if the name has no prefix.
     * <li>For any other type of node, this will return null.
     * </ul>
     * If you need to call more than one accessor, consider using {@link #getIterator()} instead -- it will be
     * faster.
     *
     * @param ordinal the ordinal of the node in this result set.  Must be nonnegative and less than {@link #size()}.
     * @return the prefix, per the above, or null if the ordinal is out of range.
     */
    String getNodePrefix(int ordinal);

    /**
     * Get the local name of the node in this nodeset with the specified ordinal, or null if that type of node
     * doesn't have a local name.
     * <ul>
     * <li>For elements or attributes, this will be the local part of the qualified name.  Never null or empty.
     * <li>For any other type of node, the behavior of this method isn't further defined.
     * </ul>
     * If you need to call more than one accessor, consider using {@link #getIterator()} instead -- it will be
     * faster.
     *
     * @param ordinal the ordinal of the node in this result set.  Must be nonnegative and less than {@link #size()}.
     * @return the local name, per the above, or null if the ordinal is out of range.
     */
    String getNodeLocalName(int ordinal);

    /**
     * Get the full name of the node in this nodeset with the specified ordinal, or null if that type of node
     * doesn't have a full name.
     * <ul>
     * <li>For elements or attributes, this is the qualified name.  Never null or empty.
     * <li>For a comment or text node, this will return null.
     * <li>For processing instructions, this is the name of the PI, e.g. <code>xml-stylesheet</code>
     * <li>For any other type of node, the behavior of this method isn't further defined.
     * </ul>
     * If you need to call more than one accessor, consider using {@link #getIterator()} instead -- it will be
     * faster.
     *
     * @param ordinal the ordinal of the node in this result set.  Must be nonnegative and less than {@link #size()}.
     * @return the node name, per the above, or null if the ordinal is out of range.
     */
    String getNodeName(int ordinal);

    /**
     * Get the "value" of the node in this nodeset with the specified ordinal, or null if there's no such node
     * or if that type of node doesn't have a value.
     * Value means different things for different node types.
     * Returns the value of this node, or null if this type of node
     * doesn't have any meaningful "value".  Contrast with getTextContent().
     *
     * <ul>
     * <li>For an element, this returns the concatenated value of all text nodes in the element and its children,
     *     per the XPath recommendation.  May be empty, but never null.
     * <li>For attributes, this is the value.  May be empty, but never null.
     * <li>For processing instructions, this is the entire string of attributes, e.g. <code>name="foo" type="bar"</code>
     * <li>For any other type of node, the behavior of this method isn't further defined.
     * </ul>
     * If you need to call more than one accessor, consider using {@link #getIterator()} instead -- it will be
     * faster.
     *
     * @param ordinal the ordinal of the node in this result set.  Must be nonnegative and less than {@link #size()}.
     * @return the value, per the above, or null if the ordinal is out of range.
     */
    String getNodeValue(int ordinal);

    /** A nodeset that is always empty. */
    static final XpathResultNodeSet EMPTY_NODESET = new XpathResultNodeSet() {
        public boolean isEmpty() {
            return true;
        }

        public int size() {
            return 0;
        }

        public XpathResultIterator getIterator() {
            return EMPTY_ITERATOR;
        }

        public int getType(int ordinal) {
            return -1;
        }

        public String getNodePrefix(int ordinal) {
            return null;
        }

        public String getNodeLocalName(int ordinal) {
            return null;
        }

        public String getNodeName(int ordinal) {
            return null;
        }

        public String getNodeValue(int ordinal) {
            return null;
        }

        protected final XpathResultIterator EMPTY_ITERATOR = new XpathResultIterator() {
            public boolean hasNext() {
                return false;
            }

            public void next(XpathResultNode template) throws NoSuchElementException {
                throw new NoSuchElementException();
            }

            public ElementCursor nextElementAsCursor() throws NoSuchElementException {
                throw new NoSuchElementException();
            }
        };
    };
}
