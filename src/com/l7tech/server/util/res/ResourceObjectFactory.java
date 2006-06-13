/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.util.res;

import java.text.ParseException;

/**
 * Interface that converts objects of type C, that came from the HTTP object cache, into objects of type R, that
 * the end user wishes to use as a resource.  If both types are the same, an implmentation of
 * {@link #createResourceObject} can simply return its argument unchanged.
 */
public interface ResourceObjectFactory<R, C> {
    /**
     * Convert the specified bytes into a resource object that can be cached and reused, including use by
     * multiple threads simulataneously.  The Object will be returned back through getResource(), and
     * the consumer can downcast it to the appropriate type.
     *
     * @param url  the URL the content was loaded from, or null
     * @param  resourceContent the content of the resource as it came from the HttpObjectCache.  Never null.
     * @return the object to cache and reuse.  Returning null will be considered the same as throwing IOException
     *         with no message.
     * @throws java.text.ParseException if the specified resource bytes could not be converted into a resource object.
     */
    R createResourceObject(String url, C resourceContent) throws ParseException;
}
