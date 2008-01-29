/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.util.res;

import java.text.ParseException;

/**
 * Interface that converts statically-configured strings into resource objects.
 */
public interface ResourceObjectFactory<R> {
    /**
     * Convert the specified String into a resource object that can be cached and reused, including use by
     * multiple threads simulataneously.  The Object will be returned back through getResource(), and
     * the consumer can downcast it to the appropriate type.
     *
     * @param  resourceString the resource as a String.  Must not be null.
     * @return the resource object.  Never returns null -- throws ParseException instead.
     * @throws java.text.ParseException if the specified resource bytes could not be converted into a resource object.
     */
    R createResourceObject(String resourceString) throws ParseException;

    /**
     * Close the given resource object.
     *
     * @param resourceObject The object (may be null)
     */
    void closeResourceObject(R resourceObject);
}
