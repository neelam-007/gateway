/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.http.cache;

import com.l7tech.common.http.*;
import org.apache.commons.collections.LRUMap;

import java.io.IOException;
import java.util.Date;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * A browser cache that ensures that only 1 thread at a time will attempt to download (and cache) a URL; other
 * threads choose whether they will wait for a download to finish or be given the previous cached object,
 * if any.
 */
public class HttpObjectCache {
    private static final Logger logger = Logger.getLogger(HttpObjectCache.class.getName());

    /**
     * Caller does not wish to wait for another thread to download the resource,
     * even if the URL has never been tried before and there is no previous cached version (or cached error message).
     */
    public static WaitMode WAIT_NEVER = new WaitMode();

    /**
     * If another thread is currently downloading the resource, caller wishes to immediately use the previous cached version
     * (or cached error message), unless there is no previous cached version (or cached error message), in which case
     * caller will wait.
     */
    public static WaitMode WAIT_INITIAL = new WaitMode();

    /**
     * If another thread is currently downloading the resource, caller wishes to wait until the download finishes,
     * and then get the newest version of the resource.
     */
    public static WaitMode WAIT_LATEST = new WaitMode();

    public static class WaitMode {
        private WaitMode() {
        }
    }

    private final LRUMap cache;
    private final long maxCacheAge;

    public HttpObjectCache(int maxCachedObjects, long maxCacheAge) {
        cache = new LRUMap(maxCachedObjects);
        this.maxCacheAge = maxCacheAge;
    }

    /**
     * The download was attempted by this or another thread but failed to produce a new user object, either due
     * to a network, HTTP, or application-level (ie, the factory returns null) failure.
     * getUserObject may still return the previous cached version, if one is available.
     * getException may be present and may have additional information about the failure.
     */
    public static final int RESULT_DOWNLOAD_FAILED = 0;

    /**
     * The download is being attempted right now by another thread, but caller passed waitForResult=false.
     * getUserObject will still return the previous cached version, if one is available.
     */
    public static final int RESULT_DOWNLOADING_NOW = 1;

    /**
     * A download was just completed successfully by this or another thread.
     * getUserObject will return the user object that was just downloaded.
     */
    public static final int RESULT_DOWNLOAD_SUCCESS = 2;

    /**
     * Either a sufficiently fresh cached object was available that no poll was even required, or a poll was performed
     * and the server said the object has not yet changed.
     * getUserObject will return the fresh user object.
     */
    public static final int RESULT_USED_CACHED = 3;


    /** Represents an entry in the object cache.  Might be a user object with or without a recent exception. */
    private static class CacheEntry {
        private boolean downloading;
        private long lastSuccessfulPollStarted; // time of last successful poll; use only if lastModified not provided
        private String lastModified; // last Modified: header from server; use in preference to lastPollStarted
        private Object userObject;
        private long userObjectCreated; // actually the time just before the HTTP request began
        private IOException exception;
        private long exceptionCreated;
    }


    public interface UserObjectFactory {
        /**
         * Create a user object from the specified HTTP response.  The response may have any status code, and may or
         * may not have a non-empty InputStream.
         *
         * @param url       the URL that was fetched to obtain this response.  Never null.
         * @param response  a non-null GenericHttpResponse, which might have any result code (not just 200).
         *                  Factory can consume its InputStream.
         * @return the user Object to enter into the cache.  Should not be null; throw IOException instead.
         * @throws IOException if this response was not accepted for caching, in which case this request will
         *                     be treated as a failure.
         */
        Object createUserObject(String url, GenericHttpResponse response) throws IOException;
    }


    /**
     * Holds the return value of a call to {@link HttpObjectCache#fetchCached}.
     */
    public static class FetchResult {
        private final int result;
        private final Object userObject;
        private final long userObjectCreated;
        private final IOException exception;
        private final long exceptionCreated;

        public FetchResult(int result, CacheEntry entry) {
            this.result = result;
            this.userObject = entry.userObject;
            this.userObjectCreated = entry.userObjectCreated;
            this.exception = entry.exception;
            this.exceptionCreated = entry.exceptionCreated;
        }

        /**
         * Get the result of this fetch through the cache.
         *
         * @return one of
         *   {@link HttpObjectCache#RESULT_DOWNLOAD_FAILED},
         *   {@link HttpObjectCache#RESULT_DOWNLOADING_NOW},
         *   {@link HttpObjectCache#RESULT_DOWNLOAD_SUCCESS}, or
         *   {@link HttpObjectCache#RESULT_USED_CACHED}.
         */
        public int getResult() {
            return result;
        }

        /**
         * Get the most up-to-date cached user object for this URL, or null if no user Object has yet been
         * obtained for this URL.
         * <p/>
         * This may be absent if {@link #getResult} is anything other than {@link HttpObjectCache#RESULT_USED_CACHED}
         * or {@link HttpObjectCache#RESULT_DOWNLOAD_SUCCESS}, but may be present even if the current download
         * failed if a previous cached version is still available.
         *
         * @return the possibly-cached user object last returned by UserObjectFactory, or null if none is available.
         */
        public Object getUserObject() {
            return userObject;
        }

        /**
         * If getUserObject is returning non-null, gets the time that this user object was created.
         * @return the time this user object was created.  Undefined if getUserObject returns null.
         */
        public long getUserObjectCreated() {
            return userObjectCreated;
        }

        /**
         * Get the error that occurred in the current download attempt, if any.  If this is non-null, it means
         * this thread or another thread just attempted a download and it resulted in the specified failure.
         * Even in this case, {@link #getUserObject} might still return non-null if an existing
         *
         * @return the IO exception that occurred in the last download attempt, if this was sufficiently recent,
         *         or null if no IO exception was recently produced by this URL.
         */
        public IOException getException() {
            return exception;
        }

        /**
         * If getException is returning non-null, gets the time that this exception was produced.
         * @return the time that this exception was caught.  Undefined if getException returns null.
         */
        public long getExceptionCreated() {
            return exceptionCreated;
        }
    }


    /**
     * Fetch the user object corresponding to this URL, using a cached version one is available.
     * <p/>
     * If the request URL uses SSL, caller is responsible for ensuring that the HTTP client and/or request
     * params are configured with an appropriately-configured SSLSocketFactory that will trust the correct server
     * cert(s) and present the correct client cert(s), as desired.
     *
     * @param httpClient            the HTTP client to fetch with.  Must not be null.
     * @param requestParams         the HTTP request parameters, suitable for a GET request.
     *                              Must be non-null and must include a non-null URL.
     * @param waitMode              Controls what to do if another thread is currently downloading this URL.  If
     *                              a new download is needed and no other thread is already downloading this URL,
     *                              this method will always do the download in the current thread.  If however another thread
     *                              is already downloading the URL, the method may wait for it to finish, depending
     *                              on the value provided for this parameter:<ul>
     *                              <li>If {@link #WAIT_INITIAL}, this method will immediate return any existing
     *                              cached object or error message; but if the URL has not yet been tried at all,
     *                              this thread will wait for the results from the downloading thread.
     *                              <li>If {@link #WAIT_LATEST},
     *                              this method will wait for the other thread to finish and then return the most
     *                              up-to-date information (a cached object and/or a cached error message).
     *                              <li>If {@link #WAIT_NEVER}, this method will immediately return the
     *                              existing cached object, if any.  Note that there might not be any cached object
     *                              or even cached error message if this URL has not been tried before.
     *                              </ul>
     * @param userObjectFactory     strategy for converting the HTTP body into an application-level object.
     *                              The returned object will be cached and returned to other fetchers of this URL
     *                              but is otherwise opaque to HttpObjectCache.
     * @return a {@link FetchResult}.  Never null.  It may contain the most recently cached user object (which
     *         <b>might not</b> be the type returned by the provided UserObjectFactory, if a different UserObjectFactory
     *         produced that request), and may contain an IOException documenting the most recent failure to download
     *         a user object.  If it contains both, the exception is always newer than the user object, meaning that
     *         the IOException was encountered downloading the latest version, but a previously cached version
     *         is available for use.
     */
    public FetchResult fetchCached(GenericHttpClient httpClient,
                                   GenericHttpRequestParams requestParams,
                                   WaitMode waitMode,
                                   UserObjectFactory userObjectFactory)
    {
        final String urlStr = requestParams.getTargetUrl().toExternalForm();

        CacheEntry entry;
        boolean shouldDownload = false;
        synchronized (cache) {
            // See if we are the first to get here
            entry = (CacheEntry)cache.get(urlStr);
            if (entry == null) {
                // We are the first thread currently interested in this URL, so we'll be doing the download
                entry = new CacheEntry();
                entry.downloading = true;
                cache.put(urlStr, entry);
                shouldDownload = true;
            }
        }

        //noinspection ConstantConditions
        assert entry != null;

        // See if we are creating the initial entry
        if (shouldDownload) {
            if (logger.isLoggable(Level.FINE)) logger.fine("URL '" + urlStr + "' is not in the cache; contacting server");
            return doPoll(entry, httpClient, requestParams, urlStr, userObjectFactory);
        }

        synchronized (entry) {
            if (entry.downloading) {
                // Another thread is currently downloading a fresh copy of the user object.  See if we need to wait.
                if (waitMode == WAIT_LATEST ||
                        (waitMode == WAIT_INITIAL && entry.userObject == null && entry.exception == null))
                {
                    // Wait for downloading to finish.
                    while (entry.downloading) {
                        try {
                            if (logger.isLoggable(Level.FINER)) logger.finer("Waiting for other thread to contact server for URL '" + urlStr + "'");
                            entry.wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("Interrupted while waiting for other thread to finish HTTP GET for URL '" + urlStr + "'", e);
                        }
                    }
                    // Return the updated info
                    return new FetchResult(null == entry.exception ? RESULT_DOWNLOAD_SUCCESS : RESULT_DOWNLOAD_FAILED,
                                           entry);
                } else {
                    // Return whatever we've got
                    if (logger.isLoggable(Level.FINER)) logger.finer("Returning cached entry for URL '" + urlStr + "' without waiting for other thread");
                    return new FetchResult(RESULT_DOWNLOADING_NOW, entry);
                }
            } else {
                // Nobody else is currently downloading a fresh copy.  See if we need to do so ourselves.
                if (!needToPoll(entry)) {
                    // Cached info is fine for now -- just return it
                    if (logger.isLoggable(Level.FINE)) logger.fine("Returning cached entry for URL '" + urlStr + "'");
                    return new FetchResult(RESULT_USED_CACHED, entry);
                }
                entry.downloading = true;
                shouldDownload = true;
                /* FALLTHROUGH and poll again */
            }
        }

        // If we get here, our lucky thread has been selected to do the next poll
        //noinspection ConstantConditions
        assert shouldDownload;

        if (logger.isLoggable(Level.FINE)) logger.fine("Cache entry for URL '" + urlStr + "' is too old; contacting server");
        return doPoll(entry, httpClient, requestParams, urlStr, userObjectFactory);
    }


    /**
     * Check if this cache entry needs to be polled again.  The rules for this are that an entry needs
     * to be polled if it both exception and user object are empty, or both exception and user object
     * are older than the current poll interval.  Caller must hold the monitor for this cache entry.
     *
     * @param entry the entry to check.  Must not be null.
     * @return true if this entry needs to be polled for up-to-date information.
     */
    private boolean needToPoll(CacheEntry entry) {
        final boolean haveObject = entry.userObject != null;
        final boolean haveException = entry.exception != null;
        if (!haveObject && !haveException) {
            // Don't have anything yet; must poll
            return true;
        }
        long now = System.currentTimeMillis();
        final boolean staleOrMissingUserObject = (!haveObject) || (now - entry.userObjectCreated) >= maxCacheAge;
        final boolean staleOrMissingException = (!haveException) || (now - entry.exceptionCreated) >= maxCacheAge;

        // Poll only if everything we've got is stale
        return staleOrMissingException && staleOrMissingUserObject;
    }


    /**
     * Synchronously poll the URL and download a new version if needed.  Caller must NOT hold the monitor
     * for entry; we'll grab it ourselves whenever we need to read or write to it.  Caller must have just
     * toggled entry.downloading from false to true, while holding the entry monitor, in this same thread,
     * just before calling this method.
     *
     * @param entry      cache entry tracking this download, which caller must have just taken write ownership of.
     * @param httpClient HTTP client to use for the request.  Must not be null.
     * @param params     HTTP request parameters, suitable for a GET request.  Must not be null.
     * @param urlStr
     * @param userObjectFactory  factory for producing the user object from the HTTP response.
     * @return the fetch result from doing the poll.  Never null.
     */
    private FetchResult doPoll(CacheEntry entry,
                               GenericHttpClient httpClient,
                               GenericHttpRequestParams params,
                               String urlStr,
                               UserObjectFactory userObjectFactory)
    {
        // We are the only thread permitted to write to the cache entry, but we'll still need to synchronize writes
        // so readers will be guaranteed to pick them up.
        long requestStart = System.currentTimeMillis();
        IOException exception;
        GenericHttpRequest req = null;
        GenericHttpResponse resp = null;
        try {
            // Prefer the server's provided modification date, since it uses the server's own clock
            final HttpHeader ifModSince;
            if (entry.lastModified != null) {
                // Use the server's own modification date
                ifModSince = new GenericHttpHeader(HttpConstants.HEADER_IF_MODIFIED_SINCE, entry.lastModified);
            } else {
                // Fall back to making one up, using our own clock  TODO fix this bug:  lastSuccessfulPollStarted is zero if there hasn't been one!
                ifModSince = GenericHttpHeader.makeDateHeader(HttpConstants.HEADER_IF_MODIFIED_SINCE,
                                                              new Date(entry.lastSuccessfulPollStarted));
            }
            params.replaceExtraHeader(ifModSince);

            req = httpClient.createRequest(GenericHttpClient.GET, params);
            resp = req.getResponse();

            if (resp.getStatus() == HttpConstants.STATUS_NOT_MODIFIED) {
                // Don't even bother doing another round-trip through the UserObjectFactory
                if (logger.isLoggable(Level.FINER)) logger.finer("Server reports no change for URL '" + urlStr + "'");
                return doSuccessfulDownload(entry, requestStart, entry.lastModified, entry.userObject);
            }

            // Save server-provided last-modified date
            HttpHeaders headers = resp.getHeaders();
            String modified = headers.getFirstValue(HttpConstants.HEADER_LAST_MODIFIED);
            Object userObject = userObjectFactory.createUserObject(params.getTargetUrl().toExternalForm(), resp);
            if (userObject != null) {
                if (logger.isLoggable(Level.FINER)) logger.finer("Downloaded new object from URL '" + urlStr + "'");
                return doSuccessfulDownload(entry, requestStart, modified, userObject);
            }

            exception = new IOException("Response not accepted for caching");
            /* FALLTHROUGH and handle the error */
        } catch (IOException e) {
            exception = e;
            /* FALLTHROUGH and handle the error */
        } finally {
            if (resp != null) resp.close();
            if (req != null) req.close();
        }

        // Record the error, wake up other threads, and return the result.
        assert exception != null;
        synchronized (entry) {
            entry.exception = exception;
            entry.exceptionCreated = System.currentTimeMillis();
            entry.downloading = false;
            entry.notifyAll();
            return new FetchResult(RESULT_DOWNLOAD_FAILED, entry);
        }
    }

    private static FetchResult doSuccessfulDownload(CacheEntry entry,
                                                    long requestStart,
                                                    String modified,
                                                    Object userObject)
    {
        // Record the success, wake up other threads, and return the result.
        synchronized (entry) {
            entry.lastSuccessfulPollStarted = requestStart;
            entry.lastModified = modified;
            entry.exception = null;
            entry.exceptionCreated = 0;
            entry.userObject = userObject;
            entry.userObjectCreated = requestStart;
            entry.downloading = false;
            entry.notifyAll();
            return new FetchResult(RESULT_DOWNLOAD_SUCCESS, entry);
        }
    }
}
