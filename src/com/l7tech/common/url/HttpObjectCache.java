/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.urlcache;

import com.l7tech.common.http.*;
import org.apache.commons.collections.LRUMap;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.IOException;
import java.net.URL;

/**
 * A browser cache that ensures that only 1 thread at a time will attempt to download (and cache) a URL; other
 * threads choose whether they will wait for a download to finish or be given the previous cached object,
 * if any.
 */
public class HttpObjectCache<UT> extends AbstractUrlObjectCache<UT> {
    private static final Logger logger = Logger.getLogger(HttpObjectCache.class.getName());

    protected final GenericHttpClientFactory httpClientFactory;
    protected final UserObjectFactory<UT> userObjectFactory;
    protected final LRUMap cache;

    /**
     * Create a cache that will keep user objects associated with URLs.
     *
     * @param maxCachedObjects      maximum number of objects that can be cached.  The cache will periodically remove
     *                              the least recently used objects as needed to keep the size below this limit.
     * @param recheckAge            if a cached object (or cached error message) is more than this many milliseconds old
     *                              when a request is issued, an If-Modified-Since: query will be issued to the URL.
     * @param httpClientFactory     factory that creates HTTP clients to fetch with.  Must not be null.
     * @param userObjectFactory     strategy for converting the HTTP body into an application-level object.
     *                              The returned object will be cached and returned to other fetchers of this URL
     *                              but is otherwise opaque to HttpObjectCache.
     * @param defaultWaitMode       default {@link AbstractUrlObjectCache.WaitMode WaitMode} when using {@link #resolveUrl(String) resolveUrl}.  If null, will use {@link AbstractUrlObjectCache#WAIT_INITIAL}.
     */
    public HttpObjectCache(int maxCachedObjects,
                           long recheckAge,
                           GenericHttpClientFactory httpClientFactory,
                           UserObjectFactory<UT> userObjectFactory,
                           WaitMode defaultWaitMode)
    {
        super(recheckAge, defaultWaitMode);
        this.httpClientFactory = httpClientFactory;
        this.userObjectFactory = userObjectFactory;
        this.cache = new LRUMap(maxCachedObjects);

        if (httpClientFactory == null || userObjectFactory == null) throw new NullPointerException();
    }

    private final Lock lock = new ReentrantLock();
    protected Lock getReadLock() { return lock; }
    protected Lock getWriteLock() { return lock; }

    protected AbstractCacheEntry<UT> cacheGet(String url) {
        return (AbstractCacheEntry<UT>)cache.get(url);
    }

    protected void cachePut(String url, AbstractCacheEntry<UT> cacheEntry) {
        cache.put(url, cacheEntry);
    }

    protected AbstractCacheEntry<UT> cacheRemove(String url) {
        return (AbstractCacheEntry<UT>)cache.remove(url);
    }

    protected DatedUserObject<UT> doGet(String urlStr,
                                            String lastModifiedStr,
                                            long lastSuccessfulPollStarted)
            throws IOException
    {
        // We are the only thread permitted to write to the cache entry, but we'll still need to synchronize writes
        // so readers will be guaranteed to pick them up.
        GenericHttpRequest req = null;
        GenericHttpResponse resp = null;
        try {
            final GenericHttpRequestParams params = new GenericHttpRequestParams(new URL(urlStr));

            // Prefer the server's provided modification date, since it uses the server's own clock
            final HttpHeader ifModSince;
            if (lastModifiedStr != null) {
                // Use the server's own modification date
                ifModSince = new GenericHttpHeader(HttpConstants.HEADER_IF_MODIFIED_SINCE, lastModifiedStr);
            } else {
                // Fall back to making one up, using our own clock  TODO fix this bug:  lastSuccessfulPollStarted is zero if there hasn't been one!
                ifModSince = GenericHttpHeader.makeDateHeader(HttpConstants.HEADER_IF_MODIFIED_SINCE,
                                                              new Date(lastSuccessfulPollStarted));
            }
            params.replaceExtraHeader(ifModSince);

            req = httpClientFactory.createHttpClient().createRequest(GenericHttpClient.GET, params);
            resp = req.getResponse();

            if (resp.getStatus() == HttpConstants.STATUS_NOT_MODIFIED) {
                // Don't even bother doing another round-trip through the UserObjectFactory
                if (logger.isLoggable(Level.FINER)) logger.finer("Server reports no change for URL '" + urlStr + "'");
                return new DatedUserObject<UT>(null, lastModifiedStr);
            }

            // Save server-provided last-modified date
            HttpHeaders headers = resp.getHeaders();
            String modified = headers.getFirstValue(HttpConstants.HEADER_LAST_MODIFIED);
            UT userObject = userObjectFactory.createUserObject(params.getTargetUrl().toExternalForm(), resp.getAsString());
            if (userObject != null) {
                if (logger.isLoggable(Level.FINER)) logger.finer("Downloaded new object from URL '" + urlStr + "'");
                return new DatedUserObject<UT>(userObject, modified);
            }

            throw new IOException("Response not accepted for caching");
        } finally {
            if (resp != null) resp.close();
            if (req != null) req.close();
        }
    }
}
