package com.l7tech.xml.xpath;

import java.util.NoSuchElementException;

/**
 * Represents a set of nodes.
 */
public interface XpathResultValueSet {

    /**
     * @eturns the value type.
     */
    short getType();

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
     * Get an iterator that will walk through the values in this set.
     *
     * @return an iterator that can be used to walk through the returned value-set.
     */
    XpathResultValueIterator getIterator();

    String[] getStringArray();

    Boolean[] getBooleanArray();

    Double[] getNumberArray();
}
