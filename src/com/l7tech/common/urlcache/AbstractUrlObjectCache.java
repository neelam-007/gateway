/*
 * Copyright (C) 2005-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.common.urlcache;

import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.mime.ContentTypeHeader;

import java.io.IOException;
import java.text.ParseException;
import java.util.concurrent.locks.Lock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A browser cache that ensures that only 1 thread at a time will attempt to download (and cache) a URL; other
 * threads choose whether they will wait for a download to finish or be given the previous cached object,
 * if any.
 * <p/>
 * This abstract class delegates the following behavior to a concrete implementation:
 *  - Cache storage and retrieval
 *  - actually fetching the user object from the URL
 * <p/>
 * The services provided by the abstract class are restricted synchronization and WaitMode behavior.
 */
public abstract class AbstractUrlObjectCache<UT> implements UrlResolver<UT> {
    private final Logger logger = Logger.getLogger(getClass().getName());

    /**
     * Caller does not wish to wait for another thread to download the resource,
     * even if the URL has never been tried before and there is no previous cached version (or cached error message).
     */
    public static final WaitMode WAIT_NEVER = new WaitMode();

    /**
     * If another thread is currently downloading the resource, caller wishes to immediately use the previous cached version
     * (or cached error message), unless there is no previous cached version (or cached error message), in which case
     * caller will wait.
     */
    public static final WaitMode WAIT_INITIAL = new WaitMode();

    /**
     * If another thread is currently downloading the resource, caller wishes to wait until the download finishes,
     * and then get the newest version of the resource.
     */
    public static final WaitMode WAIT_LATEST = new WaitMode();

    // -- Instance fields --
    protected final long maxCacheAge;
    protected final WaitMode defaultWaitMode;

    /**
     * Construct a new AbstractUrlObjectCache.
     *
     * @param maxCacheAge
     * @param defaultWaitMode
     */
    protected AbstractUrlObjectCache(long maxCacheAge, WaitMode defaultWaitMode) {
        this.maxCacheAge = maxCacheAge;
        this.defaultWaitMode = defaultWaitMode == null ? WAIT_INITIAL : defaultWaitMode;
    }

    /**
     * The download was attempted by this or another thread but failed to produce a new user object, either due
     * to a network, transport, or application-level (ie, the factory returns null) failure.
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

    /** Get the lock that protects read access to the cache.  Currently only the write lock is used by this class. */
    protected abstract Lock getReadLock();

    /** Get the lock that protects write access to the cache.  Currently only this write lock is used by this class. */
    protected abstract Lock getWriteLock();

    /** Create a new, empty cache entry. */
    protected AbstractCacheEntry<UT> newCacheEntry() {
        return new AbstractCacheEntry<UT>();
    }

    /** Find an entry in the cache, or null if it isn't there.  Caller already holds the write lock. */
    protected abstract AbstractCacheEntry<UT> cacheGet(String url);

    /** Place an entry into the cache, replacing any entry that's already there.  Caller already holds the write lock. */
    protected abstract void cachePut(String url, AbstractCacheEntry<UT> cacheEntry);

    /** Remove an entry from the cache and return it, or null if it wasn't there.  Caller already holds the write lock. */
    protected abstract AbstractCacheEntry<UT> cacheRemove(String url);

    /**
     * Called on the thread that will download a new object just before the download of the new object starts.
     * Subclasses can override this to take some action on the about-to-be-superceded object.
     * <p/>
     * This method does nothing.
     */
    protected void onStaleEntryAboutToBeReplaced(AbstractCacheEntry<UT> cacheEntry, String urlStr) {
        // Does nothing
    }

    /**
     * Fetch the user object corresponding to this URL, using a cached version one is available.
     * <p/>
     * If the request URL uses SSL, caller is responsible for ensuring that the downloading client and/or request
     * params are configured with an appropriately-configured SSLSocketFactory that will trust the correct server
     * cert(s) and present the correct client certzzz(s), as desired.
     *
     * @param urlStr                the URL to get.  Must be non-null and a valid URL.
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
     * @return a {@link com.l7tech.common.urlcache.AbstractUrlObjectCache.FetchResult}.  Never null.  It may contain the most recently cached user object (which
     *         <b>might not</b> be the type returned by the provided UserObjectFactory, if a different UserObjectFactory
     *         produced that request), and may contain an IOException documenting the most recent failure to download
     *         a user object.  If it contains both, the exception is always newer than the user object, meaning that
     *         the IOException was encountered downloading the latest version, but a previously cached version
     *         is available for use.
     */
    public FetchResult<UT> fetchCached(String urlStr, WaitMode waitMode) {
        final long threadId = Thread.currentThread().getId();
        if (waitMode == null) waitMode = defaultWaitMode;

        AbstractCacheEntry<UT> entry;
        boolean shouldDownload = false;
        final Lock writeLock = getWriteLock();
        writeLock.lock();
        try {
            // See if we are the first to get here
            entry = cacheGet(urlStr);
            if (entry == null) {
                // We are the first thread currently interested in this URL, so we'll be doing the download
                entry = newCacheEntry();
                entry.downloadingThread = threadId;
                cachePut(urlStr, entry);
                shouldDownload = true;
            } else {
                if (entry.downloadingThread == threadId) {
                    // Prevent deadlock due to recursive fetch of same URL on the same thread
                    entry = newCacheEntry();
                    entry.exception = new IOException("Recursive or circular fetch of URL: " + urlStr);
                    entry.exceptionCreated = System.currentTimeMillis();
                    return new FetchResult<UT>(500, entry);
                }
            }
        } finally {
            writeLock.unlock();
        }

        //noinspection ConstantConditions
        assert entry != null;
        assert !Thread.holdsLock(entry);

        // See if we are creating the initial entry
        if (shouldDownload) {
            if (logger.isLoggable(Level.FINE)) logger.fine("URL '" + urlStr + "' is not in the cache; contacting server");
            return doPoll(entry, urlStr);
        }

        assert entry.downloadingThread != threadId;
        synchronized (entry) {
            if (entry.downloadingThread != 0) {
                // Another thread is currently downloading a fresh copy of the user object.  See if we need to wait.
                if (waitMode == WAIT_LATEST ||
                        (waitMode == WAIT_INITIAL && entry.userObject == null && entry.exception == null))
                {
                    // Wait for downloading to finish.
                    // TODO check for deadlock due to circular download dependencies
                    while (entry.downloadingThread != 0) {
                        try {
                            if (logger.isLoggable(Level.FINER)) logger.finer("Waiting for other thread to contact server for URL '" + urlStr + "'");
                            entry.wait();  // TODO configurable max total wait time
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("Interrupted while waiting for other thread to finish downloading URL '" + urlStr + "'", e);
                        }
                    }
                    // Return the updated info
                    return new FetchResult<UT>(null == entry.exception ? RESULT_DOWNLOAD_SUCCESS : RESULT_DOWNLOAD_FAILED,
                                               entry);
                } else {
                    // Return whatever we've got
                    if (logger.isLoggable(Level.FINER)) logger.finer("Returning cached entry for URL '" + urlStr + "' without waiting for other thread");
                    return new FetchResult<UT>(RESULT_DOWNLOADING_NOW, entry);
                }
            } else {
                // Nobody else is currently downloading a fresh copy.  See if we need to do so ourselves.
                if (!needToPoll(entry)) {
                    // Cached info is fine for now -- just return it
                    if (logger.isLoggable(Level.FINE)) logger.fine("Returning cached entry for URL '" + urlStr + "'");
                    return new FetchResult<UT>(RESULT_USED_CACHED, entry);
                }
                entry.downloadingThread = threadId;
                shouldDownload = true;
                /* FALLTHROUGH and poll again */
            }
        }

        // If we get here, our lucky thread has been selected to do the next poll
        //noinspection ConstantConditions
        assert shouldDownload;

        if (logger.isLoggable(Level.FINE)) logger.fine("Cache entry for URL '" + urlStr + "' is too old; contacting server");
        boolean ok = false;
        try {
            onStaleEntryAboutToBeReplaced(entry, urlStr);
            ok = true;
        } finally {
            // onStale.. threw an unchecked exception.  We'll let it pass through, but first ensure to unlock the entry
            if (!ok) doCancelDownload(entry, urlStr);
        }
        return doPoll(entry, urlStr);
    }

    public UT resolveUrl(String url) throws IOException, ParseException {
        FetchResult<UT> result = fetchCached(url, defaultWaitMode);

        UT obj = result.getUserObject();

        // Return object if we got one, even if we have an exception that's even newer
        if (obj != null) {
            IOException e = result.getException();
            if (e != null && logger.isLoggable(Level.WARNING))
                logger.log(Level.WARNING,
                           "Reusing previously-cached copy of remote resource: URL: " + url + ": " +
                                   ExceptionUtils.getMessage(e), e);
            return obj;
        }

        // No object.  Report exception.
        IOException err = result.getException();
        if (err != null) {
            // Unwrap any wrapped ParseException
            ParseException pe = (ParseException)ExceptionUtils.getCauseIfCausedBy(err, ParseException.class);
            if (pe != null)
                throw pe;

            throw err;
        }

        // May be possible if using WAIT_NEVER
        throw new IOException("No user object available for url " + url);
    }

    /**
     * Evict from the cache any cached information about the specified URL.
     *
     * @param urlStr  the URL whose value to forget.  Must not be null.
     * @return the cached object that was just evicted from the cache, or null if no user object was cached for this URL.
     */
    public UT evict(String urlStr) {
        Lock writeLock = getWriteLock();
        writeLock.lock();
        try {
            AbstractCacheEntry<UT> entry = cacheRemove(urlStr);
            return entry == null ? null : entry.userObject;
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Check if this cache entry needs to be polled again.  The rules for this are that an entry needs
     * to be polled if it both exception and user object are empty, or both exception and user object
     * are older than the current poll interval.  Caller must hold the monitor for this cache entry.
     *
     * @param entry the entry to check.  Must not be null.
     * @return true if this entry needs to be polled for up-to-date information.
     */
    private boolean needToPoll(AbstractCacheEntry<UT> entry) {
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
     * @param urlStr     the URL to get.  Must be non-null.
     * @return the fetch result from doing the poll.  Never null.
     */
    private FetchResult<UT> doPoll(AbstractCacheEntry<UT> entry,
                                   String urlStr)
    {
        // We are the only thread permitted to write to the cache entry, but we'll still need to synchronize writes
        // so readers will be guaranteed to pick them up.
        long requestStart = System.currentTimeMillis();

        boolean reported = false;
        try {
            DatedUserObject<UT> dup = doGet(urlStr, entry.lastModified, entry.lastSuccessfulPollStarted);
            UT userObject = dup.getUserObject();
            if (userObject == null) userObject = entry.userObject;
            FetchResult<UT> ret = doSuccessfulDownload(entry, requestStart, dup.getLastModified(), userObject);
            reported = true;
            return ret;
        } catch (IOException e) {
            FetchResult<UT> ret = doFailedDownload(entry, e);
            reported = true;
            return ret;
        } finally {
            // Unchecked exceptions will pass through; just make sure we unlock the entry while they are on the way by
            if (!reported) doCancelDownload(entry, urlStr);
        }
    }

    protected static final class DatedUserObject<UT> {
        private final String lastModified;
        private final UT userObject;

        /**
         * Create an instance indicating a successful poll.  If userObject is null, the previously-cached
         * userObject will be used.
         *
         * @param userObject    the user object, or null to signal that the previous value should be reused.
         * @param lastModified  the last modified header returned from the server, or null if there wasn't one.
         *                      If this is provided, it will be kept in the cache as an opaque token and passed
         *                      back to {@link AbstractUrlObjectCache#doGet} the next time it is called on this
         *                      URL.
         */
        public DatedUserObject(UT userObject, String lastModified) {
            this.lastModified = lastModified;
            this.userObject = userObject;
        }

        public String getLastModified() {
            return lastModified;
        }

        public UT getUserObject() {
            return userObject;
        }
    }

    /**
     * Do the actual download, and either mutate entry.setLastModified() and entry.setUserObject() or throw
     * an IOException if it fails.
     *
     * @param urlStr  the URL to download.  Must not be null.
     * @return a {@link DatedUserObject} instance, that must be non-null.  See DatedUserObject for the possible
     *         configurations.
     * @throws IOException if the poll was unsuccessful.
     */
    protected abstract DatedUserObject<UT> doGet(String urlStr, String lastModifiedStr, long lastSuccessfulPollStarted) throws IOException;

    private FetchResult<UT> doFailedDownload(AbstractCacheEntry<UT> entry, IOException exception) {
        synchronized (entry) {
            entry.exception = exception;
            entry.exceptionCreated = System.currentTimeMillis();
            entry.downloadingThread = 0;
            entry.notifyAll();
            return new FetchResult<UT>(RESULT_DOWNLOAD_FAILED, entry);
        }
    }

    /** Called if an unchecked exception occurs while we are holding the entry lock. */
    private void doCancelDownload(AbstractCacheEntry<UT> entry, String urlStr) {
        synchronized (entry) {
            if (entry.exception == null) {
                entry.exception = new IOException("Unexpected internal error while attempting to download external resource: " + urlStr);
                entry.exceptionCreated = System.currentTimeMillis();
            }
            entry.downloadingThread = 0;
            entry.notifyAll();
        }
    }

    private FetchResult<UT> doSuccessfulDownload(AbstractCacheEntry<UT> entry,
                                                 long requestStart,
                                                 String modified,
                                                 UT userObject)
    {
        // Record the success, wake up other threads, and return the result.
        synchronized (entry) {
            entry.lastSuccessfulPollStarted = requestStart;
            entry.lastModified = modified;
            entry.exception = null;
            entry.exceptionCreated = 0;
            entry.userObject = userObject;
            entry.userObjectCreated = requestStart;
            entry.downloadingThread = 0;
            entry.notifyAll();
            return new FetchResult<UT>(RESULT_DOWNLOAD_SUCCESS, entry);
        }
    }

    /**
     * Specified what to do if another thread is currently downloading a URL when a request for that URL arrives
     * in another thread.  The other requests can choose either to wait for the new value to finish downloading;
     * to wait if this is the first fetch of the URL to otherwise to immediately return the previous result;
     * or to always return immediately even if there is no previous result (in which case the results will be blank,
     * containing netiehr an object nor an error message).
     *
     * @see AbstractUrlObjectCache#WAIT_NEVER
     * @see AbstractUrlObjectCache#WAIT_INITIAL
     * @see AbstractUrlObjectCache#WAIT_LATEST
     */
    public static class WaitMode {
        private WaitMode() {
        }
    }

    /**
     * Represents an entry in the object cache.  Might be a user object with or without a recent exception.
     * Concrete classes can subclass this and override {@link AbstractUrlObjectCache#newCacheEntry}
     * to store additional information right in the cache entry.  Note though that its mutex is used
     * by AbstractUrlObjectCache to keep multiple threads from reloading the same URL at the same time.
     */
    protected static class AbstractCacheEntry<UT> {
        private long downloadingThread;  // ID of downloading thread, or 0 if nobody is downloading
        private long lastSuccessfulPollStarted; // time of last successful poll; use only if lastModified not provided
        private String lastModified; // last Modified: header from server; use in preference to lastPollStarted
        private UT userObject;
        private long userObjectCreated; // actually the time just before the download request began
        private IOException exception;
        private long exceptionCreated;

        public UT getUserObject() {
            return userObject;
        }
    }

    /**
     * Source for generation of user objects.
     */
    public interface UserObjectSource {
        /**
         * Get the content type of this source.
         *
         * @return The content type (can be null if not known)
         */
        ContentTypeHeader getContentType();

        /**
         * Get the contents of the URL as a String.
         *
         * @param isXml Whether the input is an XML document or not
         * @return The String data
         * @throws IOException if an error occurs.
         */
        String getString(boolean isXml) throws IOException;

        /**
         * Get the contents of the URL as a byte array.
         *
         * @return The String data
         * @throws IOException if an error occurs.
         */
        byte[] getBytes() throws IOException;
    }

    public interface UserObjectFactory<UT> {
        /**
         * Create a user object from the specified response.  The response may have any status code, and may or
         * may not have a non-empty InputStream.
         *
         * @param url       the URL that was fetched to obtain this response.  Never null.
         * @param response  The successful response, already slurped.  Never null.
         * @return the user Object to enter into the cache.  Should not be null; throw IOException instead.
         * @throws java.io.IOException if this response was not accepted for caching, in which case this request will
         *                     be treated as a failure.
         */
        UT createUserObject(String url, UserObjectSource response) throws IOException;
    }

    /**
     * Holds the return value of a call to {@link AbstractUrlObjectCache#fetchCached}.
     */
    public static class FetchResult<UT> {
        private final int result;
        private final UT userObject;
        private final long userObjectCreated;
        private final IOException exception;
        private final long exceptionCreated;

        public FetchResult(int result, AbstractCacheEntry<UT> entry) {
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
         *   {@link AbstractUrlObjectCache#RESULT_DOWNLOAD_FAILED},
         *   {@link AbstractUrlObjectCache#RESULT_DOWNLOADING_NOW},
         *   {@link AbstractUrlObjectCache#RESULT_DOWNLOAD_SUCCESS}, or
         *   {@link AbstractUrlObjectCache#RESULT_USED_CACHED}.
         */
        public int getResult() {
            return result;
        }

        /**
         * Get the most up-to-date cached user object for this URL, or null if no user Object has yet been
         * obtained for this URL.
         * <p/>
         * This may be absent if {@link #getResult} is anything other than {@link AbstractUrlObjectCache#RESULT_USED_CACHED}
         * or {@link AbstractUrlObjectCache#RESULT_DOWNLOAD_SUCCESS}, but may be present even if the current download
         * failed if a previous cached version is still available.
         *
         * @return the possibly-cached user object last returned by UserObjectFactory, or null if none is available.
         */
        public UT getUserObject() {
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
         * Even in this case, {@link #getUserObject} might still return non-null if a previous download
         * succeeded.
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
}
