package com.l7tech.console.tree;

import java.util.Collections;
import java.util.Enumeration;
import java.util.NoSuchElementException;


/**
 * EntitiesEnumeration is a class that implements a Enumeration
 * interface. The element list retrieveal is delegated to the
 * ContextList interface passed on construction.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class EntitiesEnumeration implements Enumeration {
    private EntitiesCollection entitiesList;
    private Enumeration enumeration;


    public EntitiesEnumeration(EntitiesCollection ctxList) {
        this.entitiesList = ctxList;
    }

    /**
     * Tests if this enumeration contains more elements.
     *
     * @return  <code>true</code> if and only if this enumeration object
     *           contains at least one more element to provide;
     *          <code>false</code> otherwise.
     */
    public boolean hasMoreElements() {
        boolean result = false;
        result = hasMore();

        return result;
    }

    /**
     * Returns the next element of this enumeration if this enumeration
     * object has at least one more element to provide.
     *
     * @return     the next element of this enumeration.
     * @exception  NoSuchElementException  if no more elements exist.
     */
    public Object nextElement() throws NoSuchElementException {
        return next();
    }

    /**
     * Determines whether there are any more elements in the enumeration.
     * This method allows  exceptions encountered while determining
     * whether there are more elements to be caught and handled by the
     * application.
     *
     * @return true if there is more in the enumeration ; false otherwise.
     * @exception RuntimeException
     *                   If an exception is encountered while attempting
     *                   to determine whether there is another element in the
     *                   enumeration.
     */
    public final boolean hasMore() throws RuntimeException {
        if (enumeration == null) {
            enumeration = Collections.enumeration(entitiesList.getNextBatch());
            return enumeration.hasMoreElements();
        }

        if (!enumeration.hasMoreElements()) {
            enumeration = null;
            return hasMore();
        }
        return true;
    }

    /**
     * Retrieves the next element in the enumeration. This method allows
     * naming exceptions encountered while retrieving the next element
     * to be caught and handled by the application.
     *
     * @return the next element in the enumeration if available
     * @exception NoSuchElementException
     *                   If attempting to get the next element when none
     *                   is available.
     */
    public final Object next() throws NoSuchElementException {
        if (!hasMore()) {
            throw new NoSuchElementException();
        }
        return enumeration.nextElement();
    }
}

