/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.xml.xpath;

/**
 * Represents a node in a set of xpath results.
 */
public class XpathResultNode {
    public int type = -1;
    public Object localNameHaver;
    public Object prefixHaver;
    public Object nodeNameHaver;
    public Object nodeValueHaver;

    /**
     * Create a new XpathResultNode instance ready to be filled in by an {@link XpathResultIterator}.
     */
    public XpathResultNode() {
    }

    /**
     * Get the type of the node, represented by the short integer that
     * would have been returned by the DOM method {@link org.w3c.dom.Node#getNodeType()} if it were a DOM Node.
     *
     * @return a node type as defined by {@link org.w3c.dom.Node}, or -1 if
     *         the expression was evaluated using Tarari fastxpath and the matching node is something other than
     *         an attribute, an element, or a text node.
     */
    public int getType() {
        return type;
    }

    /**
     * Get the local name of the node, or null if this type of node
     * doesn't have a local name.
     * <ul>
     * <li>For elements or attributes, this will be the local part of the qualified name.  Never null or empty.
     * <li>For a comment or text node, this will return null.
     * <li>For any other type of node, the behavior of this method isn't further defined.
     * </ul>
     *
     * @return the local name, per the above.
     */
    public String getNodeLocalName() {
        return localNameHaver == null ? null : localNameHaver.toString();
    }

    /**
     * Get the prefix of the node, or null if it does not (or cannot)
     * have a prefix.
     * <ul>
     * <li>For elements or attributes, this will be the prefix of the qualified name, or null if the name has no prefix.
     * <li>For any other type of node, this will return null.
     * </ul>
     *
     * @return the prefix, per the above.
     */
    public String getNodePrefix() {
        return prefixHaver == null ? null : prefixHaver.toString();
    }

    /**
     * Get the full name of the node, or null if this type of node
     * doesn't have a full name.
     * <ul>
     * <li>For elements or attributes, this is the qualified name.  Never null or empty.
     * <li>For a comment or text node, this will return null.
     * <li>For processing instructions, this is the name of the PI, e.g. <code>xml-stylesheet</code>
     * <li>For any other type of node, the behavior of this method isn't further defined.
     * </ul>
     *
     * @return the node name, per the above.
     */
    public String getNodeName() {
        return nodeNameHaver == null ? null : nodeNameHaver.toString();
    }

    /**
     * Get the "value" of this node.  Value means different things for different node types.
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
     *
     * @return the value, per the above, or null if the ordinal is out of range.
     */
    public String getNodeValue() {
        return nodeValueHaver == null ? null : nodeValueHaver.toString();
    }
}
