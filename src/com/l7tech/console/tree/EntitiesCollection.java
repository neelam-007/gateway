package com.l7tech.console.tree;

import java.util.Collection;

/**
 * An object that implements the EntitiesCollection interface generates a
 * list of elements/entities.. Successive calls to the
 * getNextBatch() method return successive elements of the series.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.0
 */
public interface EntitiesCollection {
    /**
     * The general methods contract is to return the next List of elements
     * in the context.
     * If no (more) elements are available the empty list is returned. The
     * implementation of the interface decide about the batch size, when is
     * the list exhausted etc.
     *
     * @return a List representing a successive elements in the context.
     * @exception RuntimeException
     *                   thrown when there was an error in accessing elements.
     *                   The original cause/exception is encapsulated as inner
     *                   Throwable.
     */
    Collection getNextBatch() throws RuntimeException;
}
