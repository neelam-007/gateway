/*
 * Copyright (C) 2005-2007 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.url;

import java.io.IOException;
import java.text.ParseException;

/**
 * Interface implemented by a service that translates URLs into objects.
 */
public interface UrlResolver<UT> {
    /**
     * Resolve the specified URL to an object.  The precise details of how this is done (examples: network fetch every
     * time, lookup from map or config file, caching or non-caching) will depend on the specific implementation.
     *
     * @param url  the URL to resolve, as a string.  Must be non-null.
     * @return the resolved user object.  An implementation must not return null; it must instead throw an exception.
     * @throws IOException     if an object could not be created because the specified resource did not exist
     * @throws IOException     if there was a network problem connecting to the specified URL
     * @throws ParseException  if the URL was found, but what we fetched could not be converted into a user object
     *                         of the required type.
     */
    UT resolveUrl(String url) throws IOException, ParseException;
}
