/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.util.res;

import com.l7tech.common.urlcache.UrlResolver;
import com.l7tech.common.audit.Audit;

import java.io.IOException;
import java.text.ParseException;

/**
 * Superclass for ResourceGetters that fetch the resource from an external URL.
 */
abstract class UrlResourceGetter<R> extends ResourceGetter<R> {

    // --- Member fields
    private final UrlResolver<R> urlResolver;

    /**
     * Initialize common code for URL based resource getters.
     *
     * @param urlResolver   strategy for turning a URL into a resource object.  Must not be null.
     * @param audit
     */
    protected UrlResourceGetter(UrlResolver<R> urlResolver, Audit audit)
    {
        super(audit);
        if (urlResolver == null) throw new NullPointerException("A URL resolver is required.");
        this.urlResolver = urlResolver;
    }

    /**
     * Do the actual fetch of a (possibly cached) user object from the specified URL.
     *
     * @param url     the URL to fetch.  Must not be null.
     * @return the user object.  Never null.
     * @throws IOException     if an object could not be created because the specified resource did not exist
     * @throws IOException     if there was a network problem connecting to the specified URL
     * @throws ParseException  if the URL was found, but what we fetched could not be converted into a user object
     *                         of the required type.
     */
    protected R fetchObject(final String url) throws IOException, ParseException {
        return urlResolver.resolveUrl(url);
    }
}
