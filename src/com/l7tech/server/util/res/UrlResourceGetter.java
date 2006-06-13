/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.util.res;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.http.GenericHttpRequestParams;
import com.l7tech.common.http.cache.HttpObjectCache;
import static com.l7tech.common.http.cache.HttpObjectCache.WAIT_INITIAL;
import com.l7tech.common.util.ExceptionUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.text.ParseException;
import java.util.regex.Pattern;

/**
 * Superclass for ResourceGetters that fetch the resource from an external URL.
 */
abstract class UrlResourceGetter<R,C> extends ResourceGetter<R,C> {

    // --- Member fields
    private final ResourceObjectFactory<R,C> resourceObjectFactory;
    private final Auditor auditor; // may be null
    private final Pattern[] urlWhitelist; // may be null

    /**
     * Initialize common code for URL based resource getters.
     *
     * @param resourceObjectFactory  strategy for converting resource bytes into cachable user objects
     * @param urlWhitelist   whitelist of remote URL references to allow
     * @param auditor  auditor to use for logging a warning if a previously-cached version of the resource is used due to
     *                 a network problem fetching the latest version.  This auditor won't be used for any other
     *                 purpose.
     */
    protected UrlResourceGetter(ResourceObjectFactory<R,C> resourceObjectFactory,
                                HttpObjectCache<C> cache,
                                HttpObjectCache.UserObjectFactory<C> cacheObjectFactory,
                                Pattern[] urlWhitelist,
                                Auditor auditor)
    {
        super(cache, cacheObjectFactory);
        this.resourceObjectFactory = resourceObjectFactory;
        this.urlWhitelist = urlWhitelist;
        this.auditor = auditor;
    }

    /** @return the whitelist of remote URLs to allow, or null. */
    public Pattern[] getUrlWhitelist() {
        return urlWhitelist;
    }

    /**
     * Do the actual fetch of a (possibly cached) user object from this URL, through the current object cache.
     *
     * @param cache    the HttpObjectCache to use.  Must not be null.
     * @param href     the URL to fetch.  Must not be null.
     * @return the user object.  Never null.
     * @throws java.text.ParseException  if an external resource was fetched but it could not be parsed
     * @throws java.security.GeneralSecurityException  if an SSLContext is needed but cannot be initialized
     * @throws com.l7tech.server.util.res.ResourceGetter.MalformedResourceUrlException  if the provided href is not a well-formed URL
     * @throws com.l7tech.server.util.res.ResourceGetter.ResourceIOException  if there is an IOException while fetching the external resource
     * @throws NullPointerException if href or spring is null
     */
    protected R fetchObject(HttpObjectCache<C> cache, final String href)
            throws ParseException, GeneralSecurityException, ResourceIOException, MalformedResourceUrlException {
        if (href == null) throw new NullPointerException("no href provided");
        final URL url;
        try {
            url = new URL(href);
        } catch (MalformedURLException e) {
            throw new MalformedResourceUrlException("Invalid resource URL: " + href, href);
        }
        GenericHttpRequestParams params = new GenericHttpRequestParams(url);

        // Get cached, possibly checking if-modified-since against server, possibly downloading a new stylesheet
        HttpObjectCache.FetchResult<C> result = cache.fetchCached(getHttpClient(),
                                                                  params,
                                                                  WAIT_INITIAL,
                                                                  cacheObjectFactory);

        C cacheObject = result.getUserObject();
        IOException err = result.getException();

        if (cacheObject == null) {
            // Didn't manage to get a cache object.  See if we got an error instead.
            // If it's actually a ParseException, we can just unwrap and rethrow it
            Throwable pe = ExceptionUtils.getCauseIfCausedBy(err, ParseException.class);
            if (pe != null)
                throw (ParseException)pe;

            throw new ResourceIOException(err, href);
        }

        // Got a cacheObject.  See if we need to log any warnings.
        if (err != null && auditor != null)
            auditor.logAndAudit(AssertionMessages.RR_CANT_READ_REMOTE_RESOURCE, new String[] { href, ExceptionUtils.getMessage(err) });

        // Try to turn the cacheObject into a userObject.
        R userObject = resourceObjectFactory.createResourceObject(href, cacheObject);
        if (userObject == null)
            throw new ResourceIOException("Unable to create resource from HTTP response: resourceObjectFactory returned null", href);

        return userObject;
    }
}
