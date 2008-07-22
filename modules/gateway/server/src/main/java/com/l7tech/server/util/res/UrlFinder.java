/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.util.res;

import com.l7tech.xml.ElementCursor;

import java.io.IOException;

/**
 * Interface that finds the appropriate resource URL inside a message.
 */
public interface UrlFinder {
    /**
     * Inspect the specified message and return a resource URL, or null if no resource URL could be found.
     * This method is not responsible for matching any such URL against the regular expression whitelist (if any) --
     * the ResourceGetter itself has that responsibility.
     *
     * @param message  the message to inspect.  Never null.  The cursor may be moved by this method.
     * @return a URL in String form, or null if no resource URL was found in this message.
     * @throws java.io.IOException if there was a problem reading the message
     * @throws com.l7tech.server.util.res.ResourceGetter.InvalidMessageException if the message format is invalid
     */
    String findUrl(ElementCursor message) throws IOException, ResourceGetter.InvalidMessageException;
}
