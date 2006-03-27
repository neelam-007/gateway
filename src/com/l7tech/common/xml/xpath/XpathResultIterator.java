/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.xml.xpath;

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
}
