/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.xml.xpath;

import com.l7tech.xml.ElementCursor;

import java.util.NoSuchElementException;

/**
 * An iterator for walking through the nodes in an XpathResultNodeSet.
 */
public interface XpathResultIterator {
    /** @return true if there are more nodes remaining in this node set. */
    boolean hasNext();

    /**
     * Get the next node, filling the info into an XpathResultNode instance.
     * <b>Important note</b>: information filled into the template may not remain valid if next() is called
     * on this iterator again, <b><i>even if you pass it a different template instance</i></b>.
     * So, if you want to save any information, copy it somewhere else before moving to the
     * next node. 
     *
     * @param template an XPath result node which will be filled in with details.  Must not be null.
     * @throws NoSuchElementException if there are no more nodes remaining to iterate.
     */
    void next(XpathResultNode template) throws NoSuchElementException;

    /**
     * Attempts to creates and returns a new ElementCursor pointed at the next matching result node if it's an Element.
     *
     * Returns <code>null</code> if the next node is not an Element, or ElementCursor support is not available for this
     * XpathResult. (see {@link ElementCursor#getXpathResult(CompiledXpath, XpathVariableFinder, boolean)}, third argument)
     *
     * Note that this method can be quite expensive, as it creates new ElementCursors, while {@link #next} does not.
     *
     * @return null if the next matching node is not an Element or Document, or if the cursor support was not enabled
     *              for this {@link XpathResult}.
     * @throws NoSuchElementException if there are no more nodes remaining to iterate.
     */
     ElementCursor nextElementAsCursor() throws NoSuchElementException;
}
