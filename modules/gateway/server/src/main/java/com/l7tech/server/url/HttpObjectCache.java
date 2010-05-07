/*
 * Copyright (C) 2005-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.url;

import com.l7tech.common.http.*;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.io.ByteLimitInputStream;
import com.l7tech.server.ServerConfig;
import com.l7tech.util.SyspropUtil;
import com.l7tech.util.IOUtils;

import org.apache.commons.collections.map.LRUMap;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.*;
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
    public final static int DEFAULT_DOWNLOAD_LIMIT = 10 * 1024 * 1024;//10MB

    private static final Logger logger = Logger.getLogger(HttpObjectCache.class.getName());
    private static final String SYSPROP_ENABLE_LASTMODIFED = "com.l7tech.server.url.httpCacheEnableLastModified";
    private static final String SYSPROP_ENABLE_ETAG = "com.l7tech.server.url.httpCacheEnableETag";
    private static final boolean enableLastModified = SyspropUtil.getBoolean( SYSPROP_ENABLE_LASTMODIFED, true );
    private static final boolean enableETag = SyspropUtil.getBoolean( SYSPROP_ENABLE_ETAG, true );

    protected final GenericHttpClientFactory httpClientFactory;
    protected final UserObjectFactory<UT> userObjectFactory;
    protected final LRUMap cache;

    /**
     * This may be either a cluster property name or a system property
     */
    private final String maxDownloadSizeProperty;
    private final int emergencyDownloadSize;

    /**
     * Create a cache that will keep user objects associated with URLs.
     *
     * @param maxCachedObjects        maximum number of objects that can be cached.  The cache will periodically remove
     *                                the least recently used objects as needed to keep the size below this limit.
     * @param recheckAge              if a cached object (or cached error message) is more than this many milliseconds old
     *                                when a request is issued, an If-Modified-Since: query will be issued to the URL.
     * @param httpClientFactory       factory that creates HTTP clients to fetch with.  Must not be null.
     * @param userObjectFactory       strategy for converting the HTTP body into an application-level object.
     *                                The returned object will be cached and returned to other fetchers of this URL
     *                                but is otherwise opaque to HttpObjectCache.
     * @param defaultWaitMode         default {@link com.l7tech.server.url.AbstractUrlObjectCache.WaitMode WaitMode} when using {@link #resolveUrl(String) resolveUrl}.  If null, will use {@link com.l7tech.server.url.AbstractUrlObjectCache#WAIT_INITIAL}.
     * @param maxDownloadSizeProperty String the cluster property to retrieve the download limit from
     */
    public HttpObjectCache(int maxCachedObjects,
                           long recheckAge,
                           GenericHttpClientFactory httpClientFactory,
                           UserObjectFactory<UT> userObjectFactory,
                           WaitMode defaultWaitMode,
                           String maxDownloadSizeProperty) {
        this(maxCachedObjects,
                recheckAge,
                httpClientFactory,
                userObjectFactory,
                defaultWaitMode,
                maxDownloadSizeProperty,
                DEFAULT_DOWNLOAD_LIMIT);
    }

    /**
     * Create a cache that will keep user objects associated with URLs.
     *
     * @param maxCachedObjects        maximum number of objects that can be cached.  The cache will periodically remove
     *                                the least recently used objects as needed to keep the size below this limit.
     * @param recheckAge              if a cached object (or cached error message) is more than this many milliseconds old
     *                                when a request is issued, an If-Modified-Since: query will be issued to the URL.
     * @param httpClientFactory       factory that creates HTTP clients to fetch with.  Must not be null.
     * @param userObjectFactory       strategy for converting the HTTP body into an application-level object.
     *                                The returned object will be cached and returned to other fetchers of this URL
     *                                but is otherwise opaque to HttpObjectCache.
     * @param defaultWaitMode         default {@link com.l7tech.server.url.AbstractUrlObjectCache.WaitMode WaitMode} when using {@link #resolveUrl(String) resolveUrl}.  If null, will use {@link com.l7tech.server.url.AbstractUrlObjectCache#WAIT_INITIAL}.
     * @param maxDownloadSizeProperty String the cluster property to retrieve the download limit from
     * @param emergencyDownloadSize   int emergency value to use for the max download size, when the cluster property
     * referneced in maxDownloadSizeProperty provides an invalid numeric value
     */
    public HttpObjectCache(int maxCachedObjects,
                           long recheckAge,
                           GenericHttpClientFactory httpClientFactory,
                           UserObjectFactory<UT> userObjectFactory,
                           WaitMode defaultWaitMode,
                           String maxDownloadSizeProperty,
                           int emergencyDownloadSize) {
        super(recheckAge, defaultWaitMode);
        this.httpClientFactory = httpClientFactory;
        this.userObjectFactory = userObjectFactory;
        this.cache = maxCachedObjects <= 0 ? null : new LRUMap(maxCachedObjects);
        this.maxDownloadSizeProperty = maxDownloadSizeProperty;
        this.emergencyDownloadSize = emergencyDownloadSize;

        if (httpClientFactory == null || userObjectFactory == null) throw new NullPointerException();
    }

    private final Lock lock = new ReentrantLock();
    @Override
    protected Lock getReadLock() { return lock; }
    @Override
    protected Lock getWriteLock() { return lock; }

    @SuppressWarnings({"unchecked"})
    @Override
    protected AbstractCacheEntry<UT> cacheGet(String url) {
        if ( cache != null ) {
            synchronized( cache ) {
                return (AbstractCacheEntry<UT>)cache.get(url);
            }
        } else {
            return null;
        }
    }

    @Override
    protected void cachePut(String url, AbstractCacheEntry<UT> cacheEntry) {
        if ( cache != null ) {
            synchronized( cache ) {
                cache.put(url, cacheEntry);
            }
        }
    }

    @SuppressWarnings({"unchecked"})
    @Override
    protected AbstractCacheEntry<UT> cacheRemove(String url) {
        if ( cache != null ) {
            synchronized( cache ) {
                return (AbstractCacheEntry<UT>)cache.remove(url);
            }
        } else {
            return null;
        }
    }

    @SuppressWarnings({"unchecked"})
    @Override
    protected Iterator<AbstractCacheEntry<UT>> cacheIterator() {
        if ( cache != null ) {
            synchronized( cache ) {
                return new ArrayList<AbstractCacheEntry<UT>>(cache.values()).iterator();
            }
        } else {
            return null;
        }
    }

    @Override
    protected DatedUserObject<UT> doGet( final String urlStr,
                                         final String lastModifiedStr,
                                         final long lastSuccessfulPollStarted,
                                         final String currentEtag )
            throws IOException
    {
        // We are the only thread permitted to write to the cache entry, but we'll still need to synchronize writes
        // so readers will be guaranteed to pick them up.
        GenericHttpRequest req = null;
        GenericHttpResponse resp = null;
        try {
            final GenericHttpRequestParams params = new GenericHttpRequestParams(new URL(urlStr));

            if ( enableLastModified ) {
                // Prefer the server's provided modification date, since it uses the server's own clock
                final HttpHeader ifModSince;
                if (lastModifiedStr != null) {
                    // Use the server's own modification date
                    ifModSince = new GenericHttpHeader(HttpConstants.HEADER_IF_MODIFIED_SINCE, lastModifiedStr);
                } else if (lastSuccessfulPollStarted > 0) {
                    // Fall back to making one up, using our own clock
                    ifModSince = GenericHttpHeader.makeDateHeader(HttpConstants.HEADER_IF_MODIFIED_SINCE,
                                                                  new Date(lastSuccessfulPollStarted));
                } else {
                    ifModSince = null;
                }

                if ( ifModSince != null )
                    params.replaceExtraHeader(ifModSince);
            }

            if ( enableETag && currentEtag != null ) {
                final HttpHeader ifNoneMatch = new GenericHttpHeader(HttpConstants.HEADER_IF_NONE_MATCH, currentEtag);
                params.replaceExtraHeader(ifNoneMatch);
            }

            req = httpClientFactory.createHttpClient().createRequest(HttpMethod.GET, params);
            resp = req.getResponse();

            // Get server-provided last-modified date
            final HttpHeaders headers = resp.getHeaders();
            final String modified = headers.getFirstValue(HttpConstants.HEADER_LAST_MODIFIED);
            final String etag = headers.getFirstValue(HttpConstants.HEADER_ETAG);

            if (resp.getStatus() == HttpConstants.STATUS_NOT_MODIFIED) {
                // Don't even bother doing another round-trip through the UserObjectFactory
                if (logger.isLoggable(Level.FINER)) logger.finer("Server reports no change for URL '" + urlStr + "'");
                return new DatedUserObject<UT>(null, modified!=null ? modified : lastModifiedStr, etag);
            }

            final GenericHttpResponse sourceResponse = resp;
            //read max size here so that it can be modified after a HttpObjectCache has been created
            final int maxSize = ServerConfig.getInstance().getIntProperty(maxDownloadSizeProperty, emergencyDownloadSize);
            UT userObject = userObjectFactory.createUserObject(params.getTargetUrl().toExternalForm(), new UserObjectSource(){
                @Override
                public byte[] getBytes() throws IOException {
                    return IOUtils.slurpStream(new ByteLimitInputStream(sourceResponse.getInputStream(), 1024, maxSize));
                }

                @Override
                public ContentTypeHeader getContentType() {
                    return sourceResponse.getContentType();
                }

                @Override
                public String getString(boolean isXml) throws IOException {
                    return sourceResponse.getAsString(isXml, maxSize);
                }
            });
            if (userObject != null) {
                if (logger.isLoggable(Level.FINER)) logger.finer("Downloaded new object from URL '" + urlStr + "'");
                return new DatedUserObject<UT>(userObject, modified, etag);
            }

            throw new IOException("Response not accepted for caching");
        } finally {
            if (resp != null) resp.close();
            if (req != null) req.close();
        }
    }
}
