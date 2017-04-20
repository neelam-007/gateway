package com.l7tech.xml.xpath;

import com.l7tech.xml.ElementCursor;

import java.util.NoSuchElementException;

/**
 * An iterator for walking through the values in an XpathResultValueSet.
 */
public interface XpathResultValueIterator {

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
    void next(XpathResultValue template) throws NoSuchElementException;

}
