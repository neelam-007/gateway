/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.communityschemas;

import com.l7tech.common.http.*;
import com.l7tech.common.io.ByteLimitInputStream;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.server.url.AbstractUrlObjectCache;
import com.l7tech.server.url.HttpObjectCache;
import com.l7tech.util.CausedIOException;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.LSInputImpl;
import com.l7tech.util.DomUtils;
import com.l7tech.xml.TarariLoader;
import com.l7tech.xml.tarari.TarariSchemaHandler;
import com.l7tech.xml.tarari.TarariSchemaSource;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.util.HttpClientFactory;
import org.w3c.dom.Element;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.SAXException;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Creates and manages all in-memory instances of {@link CompiledSchema}, including uploading them to hardware
 * when they are unambiguous, and unloading them from hardware when they become ambiguous.
 * <p/>
 * Users of {@link CompiledSchema}s should feel free to retain {@link SchemaHandle}s for any that are
 * actually in use, but should not cache them any longer than needed; hardware acceleration
 * relies on their prompt closure.
 */
public class SchemaManagerImpl implements SchemaManager, PropertyChangeListener {
    private static final Logger logger = Logger.getLogger(SchemaManagerImpl.class.getName());

    private static class CacheConf {
        private final int maxCacheAge;
        private final int maxCacheEntries;
        private final int hardwareRecompileLatency;
        private final int hardwareRecompileMinAge;
        private final int hardwareRecompileMaxAge;
        private final long maxSchemaSize;
        private final boolean softwareFallback;

        CacheConf(ServerConfig serverConfig) {
            maxCacheAge = serverConfig.getIntProperty(ServerConfig.PARAM_SCHEMA_CACHE_MAX_AGE, 300000);
            maxCacheEntries = serverConfig.getIntProperty(ServerConfig.PARAM_SCHEMA_CACHE_MAX_ENTRIES, 100);
            hardwareRecompileLatency = serverConfig.getIntProperty(ServerConfig.PARAM_SCHEMA_CACHE_HARDWARE_RECOMPILE_LATENCY, 10000);
            hardwareRecompileMinAge = serverConfig.getIntProperty(ServerConfig.PARAM_SCHEMA_CACHE_HARDWARE_RECOMPILE_MIN_AGE, 500);
            hardwareRecompileMaxAge = serverConfig.getIntProperty(ServerConfig.PARAM_SCHEMA_CACHE_HARDWARE_RECOMPILE_MAX_AGE, 30000);
            maxSchemaSize = serverConfig.getLongProperty(ServerConfig.PARAM_SCHEMA_CACHE_MAX_SCHEMA_SIZE, 1000000L);

            // This isn't "true".equals(...) just in case ServerConfig returns null--we want to default to true.
            softwareFallback = !("false".equals(serverConfig.getProperty(ServerConfig.PARAM_SCHEMA_SOFTWARE_FALLBACK)));
        }
    }

    private final ServerConfig serverConfig;
    private final AtomicReference<CacheConf> conf;
    private final HttpClientFactory httpClientFactory;

    /** This LSInput will be returned to indicate "Resource not resolved, and don't try to get it over the network unless you know what you are doing" */
    public static final LSInput LSINPUT_UNRESOLVED = new LSInputImpl();

    private final ReadWriteLock cacheLock = new ReentrantReadWriteLock(false);

    /**
     * The latest version of every currently-known schema, by URL.  This includes SchemaEntry and policy assertion
     * statis schemas, as well as transient schemas that were loaded from the network.
     * <p/>
     * Superseded schema versions are removed from this hashmap (and the handle from the hashmap is closed).
     * A superseded schema's claim to its TNS will be released when its last user handle closes'.
     * <p/>
     * For the transient schemas loaded from the network, a periodic task removes URLs whose schemas have not
     * been used recently.
     * <p/>
     * Access is not lock-protected.
     */
    private final Map<String, SchemaHandle> schemasBySystemId = new ConcurrentHashMap<String, SchemaHandle>();

    /**
     * For each currently-active TNS, stores (weakly) the set of currently-active schemas that use that TNS.
     * Both global and remote schemas may be present in the TNS cache.
     * <p/>
     * Access is protected by {@link #cacheLock}.
     */
    private final Map<String, Map<CompiledSchema, Object>> tnsCache = new WeakHashMap<String, Map<CompiledSchema, Object>>();

    /**
     * Weak set of schemas that became hardware-eligible since the last hardware reload.
     * <p/>
     * Access is protected by {@link #cacheLock}.
     */
    private final Map<CompiledSchema, Object> schemasWaitingToLoad = new WeakHashMap<CompiledSchema, Object>();

    /**
     * Shared cache for all remotely-loaded schema strings, system-wide.
     * This is a low-level cache that stores Strings, to save network calls.
     */
    private final AtomicReference<HttpObjectCache<String>> httpStringCache;

    /**
     * Stores the latest version of each globally-registered schema string (SchemaEntry, and policy assertion
     * static schemas).
     */
    private final Map<String, String> globalSchemasByUrl = new ConcurrentHashMap<String, String>();

    /**
     * Stores the set of schema URLs that were superseded by new downloads since the last compile began.
     * (Currently schema compilation is restricted to a single thread at a time.)
     */
    private final Map<String, Integer> schemasRecentlySuperseded = new ConcurrentHashMap<String, Integer>();

    /**
     * Stores handles that should be closed next time someone is free to do so (holding no locks).
     * If nobody does it before then (such as the finalizer), the maintenance task will close these handles.
     * Don't use this directly; use the {@link #deferredCloseHandle(SchemaHandle)} method instead.
     */
    private final Map<SchemaHandle, Object> handlesNeedingClosed = new WeakHashMap<SchemaHandle, Object>();

    /** Bean that handles Tarari hardware reloads for us, null if no Tarari. */
    private final TarariSchemaHandler tarariSchemaHandler = TarariLoader.getSchemaHandler();

    private long lastHardwareRecompileTime = 0;
    private long lastSchemaEligibilityTime = 0;
    private long firstSchemaEligibilityTime = 0;

    private final Timer maintenanceTimer;
    private SafeTimerTask cacheCleanupTask;
    private SafeTimerTask hardwareReloadTask;

    private abstract static class SafeTimerTask extends TimerTask {
        public final void run() {
            try {
                doRun();
            } catch (Throwable e) {
                logger.log(Level.SEVERE, "Unexpected exception on schema cache maintenance thread: " + ExceptionUtils.getMessage(e), e);
            }
        }

        protected abstract void doRun();
    }

    /*

       Constructor

    */
    public SchemaManagerImpl(ServerConfig serverConfig, HttpClientFactory httpClientFactory, Timer timer) {
        if (serverConfig == null || httpClientFactory == null) throw new NullPointerException();
        this.serverConfig = serverConfig;
        this.httpClientFactory = httpClientFactory;
        this.conf = new AtomicReference<CacheConf>();
        this.httpStringCache = new AtomicReference<HttpObjectCache<String>>();

        if (timer == null)
            timer = new Timer("Schema cache maintenance", true);
        maintenanceTimer = timer;

        updateCacheConfiguration();
    }

    private synchronized void updateCacheConfiguration() {
        conf.set(new CacheConf(serverConfig));
        HttpObjectCache.UserObjectFactory<String> userObjectFactory = new HttpObjectCache.UserObjectFactory<String>() {
            public String createUserObject(String url, AbstractUrlObjectCache.UserObjectSource responseSource) throws IOException {
                String response = responseSource.getString(true);
                onUrlDownloaded(url);
                return response;
            }
        };

        if (this.cacheCleanupTask != null) {
            this.cacheCleanupTask.cancel();
            this.cacheCleanupTask = null;
        }

        if (this.hardwareReloadTask != null) {
            this.hardwareReloadTask.cancel();
            this.hardwareReloadTask = null;
        }

        GenericHttpClientFactory hcf = wrapHttpClientFactory(httpClientFactory, conf.get().maxSchemaSize);

        httpStringCache.set(new HttpObjectCache<String>(conf.get().maxCacheEntries,
                                                      conf.get().maxCacheAge,
                                                      hcf,
                                                      userObjectFactory,
                                                      HttpObjectCache.WAIT_LATEST));

        cacheCleanupTask = new SafeTimerTask() {
            public void doRun() {
                cacheCleanup();
            }
        };
        maintenanceTimer.schedule(cacheCleanupTask, 4539, conf.get().maxCacheAge * 2 + 263);

        if (tarariSchemaHandler != null) {
            hardwareReloadTask = new SafeTimerTask() {
                public void doRun() {
                    maybeRebuildHardwareCache();
                }
            };
            maintenanceTimer.schedule(hardwareReloadTask, 1000, conf.get().hardwareRecompileLatency);
        }
    }

    public void propertyChange(PropertyChangeEvent evt) {
        logger.info("Rebuilding schema cache due to change in cache configuration");
        try {
            updateCacheConfiguration();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error while rebuilding schema cache: " + ExceptionUtils.getMessage(e), e);
        }
    }

    private GenericHttpClientFactory wrapHttpClientFactory(final HttpClientFactory httpClientFactory, final long maxResponseSize) {
        return new GenericHttpClientFactory() {
            public GenericHttpClient createHttpClient() {
                return wrapHttpClient(httpClientFactory.createHttpClient());
            }

            public GenericHttpClient createHttpClient(int hostConnections, int totalConnections, int connectTimeout, int timeout, Object identity) {
                return wrapHttpClient(httpClientFactory.createHttpClient(hostConnections, totalConnections, connectTimeout, timeout, identity));
            }

            private GenericHttpClient wrapHttpClient(final GenericHttpClient httpClient) {
                return new GenericHttpClient() {
                    public GenericHttpRequest createRequest(GenericHttpMethod method, GenericHttpRequestParams params) throws GenericHttpException {
                        return wrapHttpRequest(httpClient.createRequest(method, params));
                    }
                };
            }

            private GenericHttpRequest wrapHttpRequest(final GenericHttpRequest request) {
                return new GenericHttpRequest() {
                    public void setInputStream(InputStream bodyInputStream) {
                        request.setInputStream(bodyInputStream);
                    }

                    public GenericHttpResponse getResponse() throws GenericHttpException {
                        return wrapHttpResponse(request.getResponse());
                    }

                    public void addParameter(String paramName, String paramValue) throws IllegalArgumentException, IllegalStateException {
                        request.addParameter(paramName, paramValue);
                    }

                    public void close() {
                        request.close();
                    }
                };
            }

            private GenericHttpResponse wrapHttpResponse(final GenericHttpResponse response) {
                return new GenericHttpResponse() {
                    public InputStream getInputStream() throws GenericHttpException {
                        return new ByteLimitInputStream(response.getInputStream(), 1024, maxResponseSize);
                    }

                    public void close() {
                        response.close();
                    }

                    public int getStatus() {
                        return response.getStatus();
                    }

                    public HttpHeaders getHeaders() {
                        return response.getHeaders();
                    }

                    public ContentTypeHeader getContentType() {
                        return response.getContentType();
                    }

                    public Long getContentLength() {
                        return response.getContentLength();
                    }
                };
            }
        };
    }

    /**
     * Report that a URL has just been downloaded.  If any previous schema with this URL was known, it and any
     * schemas that make use of it will need to be recompiled before their next use.
     *
     * @param url  the URL that was just successfully downloaded.  Must not be null.
     */
    private void onUrlDownloaded(String url) {
        SchemaHandle old = schemasBySystemId.remove(url);
        if (old != null) {
            schemasRecentlySuperseded.put(url, 1);
            deferredCloseHandle(old);
        }
    }

    private synchronized void deferredCloseHandle(SchemaHandle handle) {
        handlesNeedingClosed.put(handle, null);
    }

    private void cacheCleanup() {
        logger.finer("Running periodic schema cache cleanup task");

        closeDeferred();

        // Do some cache maintenance.  We'll decide what to do while holding the read lock;
        // then, we'll grab the write lock and do it.
        Map<String, SchemaHandle> urlsToRemove = new HashMap<String, SchemaHandle>();

        final long maxCacheEntries;

        cacheLock.readLock().lock();
        try {
            maxCacheEntries = conf.get().maxCacheEntries + globalSchemasByUrl.size();

            // First, if the cache is too big, throw out the least-recently-used schemas until it isn't.
            long extras = schemasBySystemId.size() - maxCacheEntries;
            if (extras > 0) {
                List<SchemaHandle> handles = new ArrayList<SchemaHandle>(schemasBySystemId.values());
                extras = maxCacheEntries - handles.size();
                // Have to double check in case one went away while we were copying out of the map
                if (extras > 0) {
                    Collections.sort(handles, new Comparator<SchemaHandle>() {
                        public int compare(SchemaHandle left, SchemaHandle right) {
                            CompiledSchema leftCs = left.getTarget();
                            CompiledSchema rightCs = right.getTarget();

                            if (leftCs == null && rightCs == null) return 0;
                            if (leftCs == null) return -1;
                            if (rightCs == null) return 1;

                            // Sort global schemas to the end so they never get thrown away, no matter how hoary they get
                            if (leftCs.isTransientSchema() && !rightCs.isTransientSchema())
                                return -1;
                            if (rightCs.isTransientSchema() && !leftCs.isTransientSchema())
                                return 1;

                            Long leftTime = leftCs.getLastUsedTime();
                            Long rightTime = rightCs.getLastUsedTime();
                            return leftTime.compareTo(rightTime);
                        }
                    });

                    for (int i = 0; i < extras; ++i) {
                        final SchemaHandle handle = handles.get(i);
                        final CompiledSchema schema = handle.getTarget();
                        if (schema != null) urlsToRemove.put(schema.getSystemId(), handle);
                    }
                }
            }

            // Then scan for any duplicate-TNS-causers that haven't been used in a while
            long now = System.currentTimeMillis();
            long maxAge = conf.get().maxCacheAge * 4;
            for (SchemaHandle schemaHandle : schemasBySystemId.values()) {
                CompiledSchema schema = schemaHandle.getTarget();
                if (schema != null && schema.isTransientSchema()) {
                    long lastUsed = schema.getLastUsedTime();
                    long useAge = now - lastUsed;
                    if (useAge > maxAge) {
                        // It hasn't been used in a while.  Is it contributing to a TNS conflict?
                        Map<CompiledSchema,Object> tnsUsers = tnsCache.get(schema.getTargetNamespace());
                        if (tnsUsers == null || tnsUsers.size() < 2) continue;

                        // It is Part Of The Problem.  Throw it out until someone wants it again.
                        urlsToRemove.put(schema.getSystemId(), schemaHandle);
                    }
                }
            }
        } finally {
             cacheLock.readLock().unlock();
        }

        // Now remove em
        cacheLock.writeLock().lock();
        try {
            for (Map.Entry<String, SchemaHandle> entry : urlsToRemove.entrySet()) {
                String url = entry.getKey();
                SchemaHandle handle = entry.getValue();

                // Make sure it didn't get replaced while we were waiting for the write lock
                SchemaHandle old = schemasBySystemId.get(url);
                if (old == handle) {
                    if (logger.isLoggable(Level.FINE)) logger.fine("Invalidating cached schema with systemId " + url);
                    old = schemasBySystemId.remove(url);
                    if (old != null) deferredCloseHandle(old);
                }
            }
        } finally {
            cacheLock.writeLock().unlock();
        }

        // Close all the handles we kicked out
        closeDeferred();

        // Now run through and touch all the source strings, to make sure we keep up-to-date
        for (SchemaHandle schemaHandle : schemasBySystemId.values()) {
            CompiledSchema schema = schemaHandle.getTarget();
            if (schema != null) {
                final String url = schema.getSystemId();
                try {
                    getSchemaStringForUrl(url, url, true);
                } catch (IOException e) {
                    logger.warning("Unable to update remote schema from URL -- will keep using previous value: " + url);
                }
            }
        }

        if (!schemasRecentlySuperseded.isEmpty()) {
            cacheLock.writeLock().lock();
            try {
                invalidateParentsOfRecentlySupersededSchemas();
            } finally {
                cacheLock.writeLock().unlock();
            }
        }

        // Close all the handles we kicked out
        closeDeferred();

        if (logger.isLoggable(Level.FINEST)) reportCacheContents();
    }

    /**
     * Close any handles whose closes were deferred.  Caller must not hold any locks (the monitor or cacheLock).
     */
    private void closeDeferred() {
        final List<SchemaHandle> deferredClose;
        synchronized (this ) {
            if (handlesNeedingClosed.isEmpty()) return;
            deferredClose = new ArrayList<SchemaHandle>(handlesNeedingClosed.keySet());
            handlesNeedingClosed.clear();
        }

        logger.log(Level.FINE, "Schema cache closing {0} unused schema handles", deferredClose.size());

        // Processed deferred closes while owning no locks
        for (SchemaHandle handle : deferredClose)
            if (handle != null) handle.close();
    }

    /* Caller must hold at least the read lock. */
    private static void visitAllParentsRecursive(CompiledSchema schema, Set<CompiledSchema> visited) {
        if (visited.contains(schema)) return;
        visited.add(schema);
        Set<CompiledSchema> exps = schema.getExports();
        for (CompiledSchema parent : exps) {
            if (parent != null) visitAllParentsRecursive(parent, visited);
        }
    }

    /* Caller must hold at least the read lock. */
    private void getParents(String url, Set<CompiledSchema> collected) {
        Collection<SchemaHandle> allHandles = new ArrayList<SchemaHandle>(schemasBySystemId.values());
        for (SchemaHandle schemaHandle : allHandles) {
            final CompiledSchema schema = schemaHandle.getTarget();
            if (schema == null) continue;
            Collection<SchemaHandle> imports = schema.getImports().values();
            for (SchemaHandle handle : imports) {
                final CompiledSchema impSchema = handle.getTarget();
                if (impSchema == null) continue;
                if (url.equals(impSchema.getSystemId())) {
                    visitAllParentsRecursive(impSchema, collected);
                }
            }
        }
    }

    /**
     * @return schema for URL, creating it if necessary.
     *         This is a new handle duped just for the caller; caller must close it when they are finished with it.
     */
    public SchemaHandle getSchemaByUrl(String url) throws IOException, SAXException {
        cacheLock.readLock().lock();
        try {
            SchemaHandle ret = getSchemaByUrlNoCompile(url);
            if (ret != null) return ret;
        } finally {
            cacheLock.readLock().unlock();
        }

        // Cache miss.  We'll need to compile a new instance of this schema.
        String schemaDoc = getSchemaStringForUrl(url, url, true).getStringData();
        assert schemaDoc != null;

        // We'll prevent other threads from compiling new schemas concurrently, to avoid complications with other
        // approaches:
        // No lock: Thundering herd of threads all compiling the same new schema at the same time
        // One-lock-per-schema-URL: results in deadlock:
        //     - Thread A compiling Schema A needs to import (and compile) Schema B; while at the same time,
        //     - Thread B compiling Schema B needs to import (and compile) Schema A
        cacheLock.writeLock().lock();
        try {
            // See if anyone else got it while we were waiting for the compiler mutex
            SchemaHandle ret = getSchemaByUrlNoCompile(url);
            if (ret != null) return ret;

            return compileAndCache(url, schemaDoc);
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    /**
     * Check for a cached schema.
     * <p/>
     * Caller must hold at least the read lock.
     *
     * @param url the URL to look up in the cache.  Must not be null.
     * @return the already-compiled cached schema for this url, or null if there isn't one.
     *         If a handle is returned, it will be a new handle duped just for the caller.
     *         Caller must close the handle when they are finished with it.
     */
    private SchemaHandle getSchemaByUrlNoCompile(String url) {
        SchemaHandle handle = schemasBySystemId.get(url);
        if (handle != null) {
            CompiledSchema schema = handle.getTarget();
            if (schema != null) return schema.ref();
        }
        return null;
    }

    /**
     * Get the schema text for the specified URL.  This may require a network fetch if the schema is remote.
     *
     * @param baseUrl       the base URL, for resolving url if it is relative.  Must not be null -- if there's no base URL,
     *                      pass url as the base url.
     * @param url           the URL to resolve.  Must not be null.  May be relative to baseUrl.  If it's not relative,
     *                      or if it is relative but matches as-is in the global schema cache, baseUrl will be ignored.
     * @param policyOk      if true, urls of the form "policy:whatever" will be allowed.  This should be allowed
     *                      only if this is a top-level fetch; otherwise, policies can import each others static schemas,
     *                      possibly in violation of access control partitions.
     * @return as LSInput that contians both StringData and a SystemId.  Never null.
     * @throws IOException  if schema text could not be fetched for the specified URL.
     */
    private LSInput getSchemaStringForUrl(String baseUrl, String url, boolean policyOk) throws IOException {
        if (!policyOk && url.trim().toLowerCase().startsWith("policy:"))
            throw new IOException("Schema URL not permitted in this context: " + url);

        // Find any global schema strings
        String schemaDoc = globalSchemasByUrl.get(url);
        if (schemaDoc != null)
            return makeLsInput(url, schemaDoc);

        // Try to produce an absolute URL
        url = computeEffectiveUrl(baseUrl, url);

        // Try global cache again, with newly-qualified URL
        schemaDoc = globalSchemasByUrl.get(url);
        if (schemaDoc != null)
            return makeLsInput(url, schemaDoc);

        // Not a global schema -- do a remote schema load    TODO url whitelist goes here somewhere
        AbstractUrlObjectCache.FetchResult<String> result =
                httpStringCache.get().fetchCached(url, HttpObjectCache.WAIT_LATEST);

        schemaDoc = result.getUserObject();
        if (schemaDoc != null)
            return makeLsInput(url, schemaDoc);

        // Check for errors we should report
        IOException e = result.getException();
        if (e != null)
            throw new CausedIOException("Unable to download remote schema " + describeResource(baseUrl, url) +  " : " + ExceptionUtils.getMessage(e), e);

        // Shouldn't happen
        throw new IOException("Unable to download remote schema " + describeResource(baseUrl, url));
    }

    /** @return an LSInput that contains StringData and a SystemId. */
    private LSInput makeLsInput(String url, String schemaDoc) {
        LSInputImpl lsi =  new LSInputImpl();
        lsi.setStringData(schemaDoc);
        lsi.setSystemId(url);
        return lsi;
    }

    /**
     * Add this schema to the TNS cache, and enable hardware if this is the only schema using it and
     * only if all child schemas are already hardware-enabled.
     * <p/>
     * Caller must hold the write lock.
     *
     * @param newSchema the schema for which to maybe enable hardware.
     */
    private void maybeEnableHardwareForNewSchema(CompiledSchema newSchema) {
        if (newSchema == null) return;
        // Try to enable all children bottom-up
        for (SchemaHandle child : newSchema.getImports().values())
            maybeEnableHardwareForNewSchema(child.getTarget());

        String tns = newSchema.getTargetNamespace();
        if (tns == null) return;

        // The new CompiledSchema has a TNS, and may be hardware-accelerated if no other schema current has the same TNS.
        Map<CompiledSchema, Object> compiledSchemasWithThisTns = tnsCache.get(tns);
        if (compiledSchemasWithThisTns == null) {
            compiledSchemasWithThisTns = new WeakHashMap<CompiledSchema, Object>();
            tnsCache.put(tns, compiledSchemasWithThisTns);
        }

        compiledSchemasWithThisTns.put(newSchema, null);
        if (compiledSchemasWithThisTns.size() == 2) {
            // We're the first duplicate; disable hardware for all known CompiledSchemas with this tns
            logger.log(Level.INFO, "A second schema was found with targetNamespace {0}; neither is now eligible for hardware acceleration", tns);
            for (CompiledSchema schema : compiledSchemasWithThisTns.keySet()) {
                schema.setUniqueTns(false);
                hardwareDisable(schema);
            }
            return;
        }

        if (compiledSchemasWithThisTns.size() == 1) {
            newSchema.setUniqueTns(true);
            maybeHardwareEnable(newSchema, false, false, new HashSet<CompiledSchema>());
        } else if (compiledSchemasWithThisTns.isEmpty()) {
            logger.log(Level.FINE, "No more schemas with targetNamespace {0}", tns);
            tnsCache.remove(tns);
        }
    }

    Lock getReadLock() {
        return cacheLock.readLock();
    }

    public void registerSchema(String globalUrl, String schemadoc) {
        if (schemadoc == null) throw new NullPointerException("A schema must be provided");
        String old = globalSchemasByUrl.put(globalUrl, schemadoc);
        if (old != null)
            onUrlDownloaded(globalUrl);
    }

    public void unregisterSchema(String globalUrl) {
        String old = globalSchemasByUrl.remove(globalUrl);
        if (old != null)
            onUrlDownloaded(globalUrl);
    }

    private String describeResource(String baseURI, String url) {
        return describeResource(baseURI, url, null, null);
    }

    private String describeResource(String baseURI, String systemId, String publicId, String namespaceURI) {
        String description = null;

        if (systemId != null) {
            String resourceUrl = systemId;
            if (baseURI != null) {
                try {
                    // build url for use in description only
                    resourceUrl = new URI(baseURI).resolve(systemId).toString();
                } catch (URISyntaxException e) {
                    if (logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE,
                                "Unable to resolve url ''{0}'', relative to base url ''{1}''.",
                                new String[]{systemId, baseURI});
                    }
                } catch (IllegalArgumentException e) {
                    if (logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE,
                                "Unable to resolve url ''{0}'', relative to base url ''{1}''.",
                                new String[]{systemId, baseURI});
                    }
                }
            }

            description = "URL:" + resourceUrl;
        }
        else if (publicId != null) {
            description = "PublicID:" + publicId;
        }
        else if (namespaceURI != null) {
            description = "Namespace:" + namespaceURI;
        }
        else {
            description = "Unknown";
        }

        return description;
    }

    private static class UnresolveableException extends RuntimeException {
        private final String resourceDescription;

        public UnresolveableException(Throwable cause, String resourceDescription) {
            super(cause);
            this.resourceDescription = resourceDescription;
        }

        public String getMessage() {
            return "Unable to resolve resource " + resourceDescription;
        }

        public String getResourceDescription() {
            return resourceDescription;
        }
    }

    /**
     * Analyze the specified schema, including fetching remote references if necessary, to produce
     * an up-to-date and comprehensive set of all dependent schemas.  If this returns,
     * all child and gradchild (etc) schema strings will be hot in the httpStringCache.
     *
     * @param systemId
     * @param schemadoc
     * @return the imports directly or indirectly used by this schema.  Never null.
     * @throws SAXException if a schema is not valid
     * @throws IOException if a remote schema cannot be fetched
     */
    private Set<String> precacheSchemaDependencies(String systemId, String schemadoc) throws SAXException, IOException {
        final Set<String> imports = new HashSet<String>();

        SchemaFactory sf = SchemaFactory.newInstance(XmlUtil.W3C_XML_SCHEMA);
        LSResourceResolver lsrr = new LSResourceResolver() {
            public LSInput resolveResource(String type,
                                           String namespaceURI,
                                           String publicId,
                                           String systemId,
                                           String baseURI)
            {
                try {
                    if (systemId == null) throw new CausedIOException("No systemId, cannot resolve resource");

                    LSInput lsi = getSchemaStringForUrl(baseURI, systemId, false);
                    assert lsi != null;
                    imports.add(lsi.getSystemId());
                    return lsi;
                } catch (IOException e) {
                    throw new UnresolveableException(e, describeResource(baseURI, systemId, publicId, namespaceURI));
                }
            }
        };
        sf.setResourceResolver(lsrr);

        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(schemadoc.getBytes("UTF-8"));
            sf.newSchema(new StreamSource(bais, systemId)); // populates imports as side-effect
            return imports;
        } catch (RuntimeException e) {
            UnresolveableException unres = (UnresolveableException)ExceptionUtils.getCauseIfCausedBy(e, UnresolveableException.class);
            if (unres != null) throw new CausedIOException("Unable to resolve remote subschema for resource " + unres.getResourceDescription(), unres.getCause());
            throw e;
        }
    }

    /**
     * Compute effective URL when relative is evaluated in the context of base.
     *
     * @param base      base url, ie "http://foo.com/blah/blortch.xsd".  Must not be null.
     * @param relative  URL that may be relative to base (ie, "bloo/bletch.xsd") or may be absolute.  Must not be null.
     * @return  the effective URL when relative is evaluated relative to base.  Never null.
     * @throws NullPointerException if either base or relative is null.
     * @throws MalformedURLException if base is not an absolute URL,
     *                                   or is absolute but uses a protocol other than "http" or "https"
     * @throws MalformedURLException if relative is absolute and uses an unknown protocol
     */
    private String computeEffectiveUrl(String base, String relative) throws MalformedURLException {
        if (base == null || relative == null) throw new NullPointerException();

        // First check if the "relative" uri is in fact absolute, in which case we avoid
        // parsing the base as a URL in case it is relative.
        try {
            final URI relativeUri = new URI(relative);
            if (relativeUri.isAbsolute()) {
                final String proto = relativeUri.getScheme();
                if (!proto.equals("http") && !proto.equals("https"))
                    throw new MalformedURLException("Refusing remote schema reference with non-HTTP(S) base URL: " + base);
                return relativeUri.toURL().toExternalForm();
            }
        }
        catch(URISyntaxException use) {
            // we'll find out below ...
        }

        final URL baseUrl = new URL(base);

        final String proto = baseUrl.getProtocol();
        if (!proto.equals("http") && !proto.equals("https"))
            throw new MalformedURLException("Refusing remote schema reference with non-HTTP(S) base URL: " + base);

        return new URL(baseUrl, relative).toExternalForm();
    }

    /**
     * Compile this schema document.  This may require fetching remote schemas.
     * Caller must hold the write lock.
     *
     * @param systemId
     * @param schemadoc
     * @return a new handle, duped just for the caller.  Caller must close it when they are finished with it.
     * @throws SAXException
     */
    private SchemaHandle compileAndCache(String systemId, String schemadoc)
            throws SAXException, IOException
    {
        // Do initial parse and get deps (strings are all hot in the HTTP cache after this)
        precacheSchemaDependencies(systemId, schemadoc);

        invalidateParentsOfRecentlySupersededSchemas();

        return compileAndCacheRecursive(systemId, schemadoc, new HashSet<String>());
    }

    /** Caller must hold the write lock. */
    private void invalidateParentsOfRecentlySupersededSchemas() {
        // Invalidate parents of anything reloaded since last time we checked something
        while (!schemasRecentlySuperseded.isEmpty()) {
            List<String> strings = new ArrayList<String>(schemasRecentlySuperseded.keySet());
            schemasRecentlySuperseded.clear();
            for (String url : strings) {
                // This guy was reloaded or thrown out.  Make sure all parents are invalidated.
                Set<CompiledSchema> parents = new HashSet<CompiledSchema>();
                getParents(url, parents);
                for (CompiledSchema schema : parents) {
                    if (!schema.isClosed()) onUrlDownloaded(schema.getSystemId());
                }
            }
        }
    }

    /**
     * Build a new CompiledSchema for the specified URL and enter it into the cache.  Caller guarantees
     * that any remote schemas are hot in the cache, and any schemas that are in need of a recompile due to
     * changed remote schemas have already been invalidated and removed from schemasBySystemId.
     * <p/>
     * This will recursively compile the imported schemas.
     * <p/>
     * Caller must hold the write lock.
     *
     * @param systemId    systemId of the schema being compiled. required
     * @param schemadoc   a String containing the schema XML
     * @param seenSystemIds  the set of system IDs seen since the current top-level compile began
     * @return a SchemaHandle to a new CompiledSchema instance, already duplicated for the caller.  Caller must close
     *         this handle when they are finished with it.
     */
    private SchemaHandle compileAndCacheRecursive(String systemId, String schemadoc, final Set<String> seenSystemIds) throws SAXException, IOException {
        if (seenSystemIds.contains(systemId)) {
            //remove this exception and replace with patch to log circular imports
            //throw new SAXException("Circular imports detected.  Schema sets with circular imports are not currently supported.");
            logger.info("Circular import detected.");
            return null;
        }

        seenSystemIds.add(systemId);

        // Reparse, building up CompiledSchema instances as needed from the bottom up
        SchemaFactory sf = SchemaFactory.newInstance(XmlUtil.W3C_XML_SCHEMA);

        final Map<String,SchemaHandle> directImports = new HashMap<String,SchemaHandle>();
        final LSResourceResolver lsrr = new LSResourceResolver() {
            public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
                try {
                    if (systemId == null) throw new CausedIOException("No systemId, cannot resolve resource");

                    LSInput lsi = getSchemaStringForUrl(baseURI, systemId, false);
                    assert lsi != null;

                    SchemaHandle handle = schemasBySystemId.get(lsi.getSystemId());

                    if (handle == null) {
                        // Have to compile a new one
                        handle = compileAndCacheRecursive(lsi.getSystemId(), lsi.getStringData(), seenSystemIds);
                    } else {
                        // Can't give it away while it remains in the cache -- need to dupe it (Bug #2926)
                        handle = handle.getCompiledSchema().ref();
                    }

                    //if handle is null, there was a circular import.
                    if ( handle != null ) {
                        directImports.put(handle.getCompiledSchema().getSystemId(), handle); // give it away without closing it
                        return makeLsInput(handle.getCompiledSchema().getSystemId(), handle.getCompiledSchema().getSchemaDocument());
                    }
                    return null;

                } catch (IOException e) {
                    throw new UnresolveableException(e, describeResource(baseURI, systemId, publicId, namespaceURI));
                } catch (SAXException e) {
                    throw new UnresolveableException(e, describeResource(baseURI, systemId, publicId, namespaceURI));
                }
            }
        };
        sf.setResourceResolver(lsrr);

        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(schemadoc.getBytes("UTF-8"));
            Schema softwareSchema = sf.newSchema(new StreamSource(bais, systemId));
            String tns = XmlUtil.getSchemaTNS(schemadoc);
            Element mangledElement = DomUtils.normalizeNamespaces(XmlUtil.stringToDocument(schemadoc).getDocumentElement());
            String mangledDoc = XmlUtil.nodeToString(mangledElement);
            CompiledSchema newSchema =
                    new CompiledSchema(tns, systemId, schemadoc, mangledDoc, softwareSchema, this,
                            directImports, true, conf.get().softwareFallback);
            for (SchemaHandle directImport : directImports.values()) {
                final CompiledSchema impSchema = directImport.getCompiledSchema();
                if (impSchema == null) continue;
                impSchema.addExport(newSchema);
            }

            SchemaHandle cacheRef = newSchema.ref(); // make a handle for the cache
            SchemaHandle old = schemasBySystemId.put(newSchema.getSystemId(), cacheRef);
            if (old != null) deferredCloseHandle(old);
            maybeEnableHardwareForNewSchema(newSchema);
            return newSchema.ref(); // make a handle for the caller

        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } catch (XmlUtil.BadSchemaException e) {
            throw new SAXException("Unable to parse Schema", e);
        } catch (RuntimeException e) {
            UnresolveableException unres = (UnresolveableException) ExceptionUtils.getCauseIfCausedBy(e, UnresolveableException.class);
            if (unres != null) throw new CausedIOException("Unable to resolve remote subschema for resource " + unres.getResourceDescription(), unres.getCause());
            throw e;
        }
    }

    /**
     * Removes a CompiledSchema from caches.  If the targetNamespace of the schema to be
     * removed was previously duplicated, and a single survivor is left, it will be promoted
     * to hardware-accelerated.
     * <p/>
     * Caller must NOT hold the write lock.  Caller will typically be either and end user thread,
     * the cache cleanup thread, or a finalizer thread.
     */
    void closeSchema(CompiledSchema schema) {
        reportCacheContents();

        logger.log(Level.FINE, "Closing {0}", schema);
        String tns = schema.getTargetNamespace();

        cacheLock.writeLock().lock();
        try {

            SchemaHandle old = schemasBySystemId.get(schema.getSystemId());
            if (old != null && old.getTarget() == schema) {
                // We were the active schema for this URL -- remove ourselves
                schemasBySystemId.remove(schema.getSystemId());
                schemasRecentlySuperseded.put(schema.getSystemId(), 1);
            }

            if (tns == null) return;

            // The schema had a TNS, and might have been relevant to hardware
            hardwareDisable(schema);
            Map<CompiledSchema, Object> schemas = tnsCache.get(tns);
            if (schemas == null) return;
            schemas.remove(schema);
            if (schemas.size() == 1) {
                // Survivor gets hardware privileges
                if (logger.isLoggable(Level.INFO))
                    logger.log(Level.INFO, "Sole remaining schema with tns {0} is now eligible for hardware acceleration", schema.getTargetNamespace());
                Iterator<CompiledSchema> i = schemas.keySet().iterator();
                // Unlikely to throw ConcurrentModificationException, since all finalizers use this method and take out the write lock
                if (i.hasNext()) {
                    CompiledSchema survivingSchema = i.next();
                    survivingSchema.setUniqueTns(true);
                    maybeHardwareEnable(survivingSchema, true, true, new HashSet<CompiledSchema>());
                }
            }
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    /**
     * Find the Roots of this schema: that is, set of schemas that directly or indirectly include this schema,
     * but that themselves are not included by any other known schema.
     * @param schema the schema whose roots to find.  Must not be null.
     * @param foundRoots a Set to which found roots will be added.  Must not be null.
     */
    private void getRoots(CompiledSchema schema, Set<CompiledSchema> foundRoots) {
        Set<CompiledSchema> exps = schema.getExports();
        boolean haveExport = false;
        for (CompiledSchema parent : exps) {
            if (parent == null) continue;
            haveExport = true;
            getRoots(parent, foundRoots);
        }
        if (!haveExport) foundRoots.add(schema);
    }

    private void reportNode(CompiledSchema schema, Set<CompiledSchema> visited, StringBuilder sb, String indent) {
        visited.add(schema);
        sb.append(indent).append(schema);
        for (int i = indent.length(); i < 14; ++i) sb.append(" ");
        sb.append(schema.getTnsGen()).append("\n");
        Map<String,SchemaHandle> kids = schema.getImports();
        for (SchemaHandle schemaHandle : kids.values()) {
            CompiledSchema kid = schemaHandle.getTarget();
            if (kid != null)
                reportNode(kid, visited, sb, "    " + indent);
        }
    }

    /** Caller must NOT hold the write lock. */
    private void reportCacheContents() {
        if (!logger.isLoggable(Level.FINEST))
            return;
        
        StringBuilder sb = new StringBuilder("\n\nSchema cache contents: \n\n");
        Set<CompiledSchema> schemaSet = new HashSet<CompiledSchema>();
        Set<CompiledSchema> reported = new HashSet<CompiledSchema>();
        cacheLock.readLock().lock();
        try {
            // We'll do two passes.  In the first pass, we'll draw trees down from the roots.
            // In second pass, we'll draw any schemas that we missed during the first pass (possible
            // due to upward links being weak references)

            // First pass: find all roots, then draw the trees top-down
            final Collection<SchemaHandle> allSchemas = schemasBySystemId.values();
            for (SchemaHandle ref : allSchemas) {
                if (ref == null) continue;
                CompiledSchema cs = ref.getTarget();
                if (cs == null) continue;
                getRoots(cs, schemaSet);
            }

            for (CompiledSchema schema : schemaSet) {
                reportNode(schema, reported, sb, " -");
            }

            // Second pass: draw any that we missed
            for (SchemaHandle ref : allSchemas) {
                if (ref == null) continue;
                CompiledSchema cs = ref.getTarget();
                if (cs == null) continue;
                if (!reported.contains(cs))
                    reportNode(cs, reported, sb, "?-");
            }


            // Now draw the TNS cache
            sb.append("\n\nTNS cache:\n");
            for (Map.Entry<String, Map<CompiledSchema, Object>> entry : tnsCache.entrySet()) {
                String tns = entry.getKey();
                Map<CompiledSchema,Object> ss = entry.getValue();
                if (tns == null || ss == null || ss.isEmpty()) continue;
                sb.append("  TNS:").append(tns).append("\n");
                for (CompiledSchema schema : ss.keySet()) {
                    if (schema == null) continue;
                    final SchemaHandle current = schemasBySystemId.get(schema.getSystemId());
                    String active = (current != null && current.getTarget() == schema) ? "*" : " ";
                    sb.append("       ").append(active).append(schema).append("\n");
                }
            }
        } finally {
            cacheLock.readLock().unlock();
        }
        sb.append("\n\n");
        logger.finest(sb.toString());
    }

    /**
     * Check if this schema can be hardware-enabled
     * Caller must hold write lock.
     */
    private boolean maybeHardwareEnable(CompiledSchema schema, boolean notifyParents, boolean notifyChildren, Set<CompiledSchema> visited) {
        // Short-circuit if we know we have no work to do
        if (tarariSchemaHandler == null) return false;
        boolean notYetVisited = visited.add(schema);
        assert notYetVisited;
        if (!schema.isUniqueTns()) return false;
        if (schema.isRejectedByTarari()) return false;
        if (schema.isHardwareEligible()) return false;

        // Check that all children are loaded
        Map<String,SchemaHandle> imports = schema.getImports();
        for (SchemaHandle directImport : imports.values()) {
            final CompiledSchema cs = directImport.getTarget();
            if (cs == null) continue;
            if (visited.contains(cs)) continue; // prevent infinite downwards and upwards recursion
            if (!cs.isHardwareEligible()) {
                if (!notifyChildren) {
                    logger.log(Level.FINE, "Unable to enable hardware eligibility for schema {0} because at least one of its imports is not already hardware eligible", schema);
                    return false;
                }

                if (!maybeHardwareEnable(cs, notifyParents, true, visited)) {
                    logger.log(Level.FINE, "Unable to enable hardware eligibility for schema {0} because at least one of its imports could not be made hardware eligible", schema);
                    return false;
                }
            }
        }

        // Schedule the actual hardware load for this schema
        String tns = schema.getTargetNamespace();
        scheduleLoadHardware(schema);
        logger.log(Level.INFO, "Schema with targetNamespace {0} is eligible for hardware acceleration", tns);
        schema.setHardwareEligible(true);

        if (notifyParents) {
            // Let all our parents know that they might be hardware-enableable
            for (CompiledSchema export : schema.getExports()) {
                if (visited.contains(export)) continue;
                maybeHardwareEnable(export, notifyParents, notifyChildren, visited);
            }
        }
        return true;
    }

    /**
     * Schedule a reload of the hardware schema cache to include the specified schema, which the caller asserts
     * has just become hardware-eligible.
     * <p/>
     * Caller must hold the write lock.
     *
     * @param schema  the schema that is now waiting to load into hardware
     */
    private void scheduleLoadHardware(CompiledSchema schema) {
        schemasWaitingToLoad.put(schema, null);

        final long now = System.currentTimeMillis();
        if (firstSchemaEligibilityTime == 0)
            firstSchemaEligibilityTime = now;
        lastSchemaEligibilityTime = now;
        scheduleOneShotRebuildCheck(conf.get().hardwareRecompileMinAge);
    }

    /** Schedule a one-shot call to maybeRebuildHardwareCache(), delay ms from now. */
    private void scheduleOneShotRebuildCheck(long delay) {
        if (delay < 1) throw new IllegalArgumentException("Rebuild check delay must be positive");
        if (maintenanceTimer != null) {
            SafeTimerTask task = new SafeTimerTask() {
                public void doRun() {
                    maybeRebuildHardwareCache();
                }
            };
            maintenanceTimer.schedule(task, delay);
        }
    }

    /**
     * Check if hardware cache should be rebuilt right now.
     * Caller must hold either the read lock or write lock.
     * If schemasWaitingToLoad is non-empty, this method may still return false (postponing the rebuild), but
     *
     * @return true if the hardware cache should be rebuilt immediately.
     *         false if the hardware cache does not need to be rebuilt at this time.  If there are schemas
     *         waiting to be rebuilt, a new schema check is scheduled occur in the near future.
     */
    private boolean shouldRebuildNow() {
        if (schemasWaitingToLoad.size() < 1)
            return false;

        int hardwareRecompileLatency = conf.get().hardwareRecompileLatency;
        int hardwareRecompileMinAge = conf.get().hardwareRecompileMinAge;
        int hardwareRecompileMaxAge = conf.get().hardwareRecompileMaxAge;

        final long beforeTime = System.currentTimeMillis();

        final long msSinceLastRebuild = beforeTime - lastHardwareRecompileTime;
        if (msSinceLastRebuild < hardwareRecompileLatency) {
            final long delay = hardwareRecompileLatency - msSinceLastRebuild;
            logger.log(Level.FINER,
                       "Too soon for another hardware schema cache rebuild -- will try again in {0} ms", delay);
            scheduleOneShotRebuildCheck(delay);
            return false;
        }

        boolean forceReload = false;
        if (firstSchemaEligibilityTime > 0) {
            final long ageOfOldestSchema = beforeTime - firstSchemaEligibilityTime;
            if (ageOfOldestSchema > hardwareRecompileMaxAge) {
                logger.log(Level.FINER,  "Hardware eligible schema has been awaiting reload for {0} ms -- forcing hardware schema cache reload now");
                forceReload = true;
            }
        }

        final long ageOfNewestSchema = beforeTime - lastSchemaEligibilityTime;
        if (ageOfNewestSchema < hardwareRecompileMinAge && !forceReload) {
            long delay = hardwareRecompileMinAge - ageOfNewestSchema;
            if (delay < 100) delay = 100;
            logger.log(Level.FINEST, "New schema just became hardware eligible -- postponing hardware schema cache rebuild for {0} ms", delay);
            scheduleOneShotRebuildCheck(delay);
            return false;
        }

        return true;
    }


    /**
     * Called periodically to see if it is time to rebuild the hardware schema cache.
     */
    private void maybeRebuildHardwareCache() {
        // Do an initial fast check to see if there is anything to do, before getting the concurrency-killing write lock
        cacheLock.readLock().lock();
        try {
            if (!shouldRebuildNow()) return;
        } finally {
            cacheLock.readLock().unlock();
        }

        // Grab the write lock and do one final check, then do the rebuild
        cacheLock.writeLock().lock();
        try {
            if (!shouldRebuildNow()) return;

            // Do the actual
            doRebuild();
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    /**
     * Do the actual, immediate rebuild of the hardware schema cache.
     * Caller must hold the write lock.
     */
    private void doRebuild() {
        final long beforeTime = logger.isLoggable(Level.FINE) ? System.currentTimeMillis() : 0;

        // Produce list of schemas sorted so that includes appear before the schemas that include them
        LinkedHashMap<String, CompiledSchema> forHardware = new LinkedHashMap<String, CompiledSchema>();
        Set<CompiledSchema> alreadyAdded = new HashSet<CompiledSchema>();

        for (SchemaHandle handle : schemasBySystemId.values()) {
            CompiledSchema schema = handle.getTarget();
            if (schema == null) continue;
            schema.setLoaded(false);
            putAllHardwareEligibleSchemasDepthFirst(alreadyAdded, forHardware, schema);
        }

        if (logger.isLoggable(Level.INFO)) logger.info("Rebuilding hardware schema cache: loading " + forHardware.size() + " eligible schemas (" + schemasWaitingToLoad.size() + " new)");

        //noinspection unchecked
        Map<? extends TarariSchemaSource,Exception> failedSchemas =
                tarariSchemaHandler.setHardwareSchemas(forHardware);

        lastHardwareRecompileTime = System.currentTimeMillis(); // get time again after

        if (beforeTime > 0)
            logger.log(Level.FINE, "Rebuild of hardware schema cache took {0} ms", lastHardwareRecompileTime - beforeTime);

        for (Map.Entry<? extends TarariSchemaSource,Exception> entry : failedSchemas.entrySet()) {
            CompiledSchema schema = (CompiledSchema)entry.getKey();
            Exception error = entry.getValue();
            if (logger.isLoggable(Level.WARNING))
                logger.log(Level.WARNING, "Schema for target namespace \"" + schema.getTargetNamespace() + "\" cannot be hardware accelerated: " + ExceptionUtils.getMessage(error));
            hardwareDisable(schema);
        }

        schemasWaitingToLoad.clear();
        firstSchemaEligibilityTime = 0;
    }

    /** Recursively populate linkedhashmap of hardware-eligible schemas ordered so that includes come before their includers. */
    private void putAllHardwareEligibleSchemasDepthFirst(Set<CompiledSchema> alreadyAdded,
                                                         LinkedHashMap<String, CompiledSchema> schemas,
                                                         CompiledSchema schema)
    {
        Map<String,SchemaHandle> imports = schema.getImports();
        for (SchemaHandle imp : imports.values()) {
            final CompiledSchema impSchema = imp.getTarget();
            if (impSchema == null) continue;
            putAllHardwareEligibleSchemasDepthFirst(alreadyAdded, schemas, impSchema);
        }

        if (schema.isHardwareEligible() && !alreadyAdded.contains(schema))
            schemas.put(schema.getTargetNamespace(), schema);
    }

    /**
     * Caller must hold write lock
     */
    private void hardwareDisable(CompiledSchema schema) {
        if (tarariSchemaHandler == null) return;

        if (logger.isLoggable(Level.INFO))
            logger.log(Level.INFO, "Disabling hardware acceleration eligibility for schema with tns {0}",
                       schema.getTargetNamespace());

        schemasWaitingToLoad.remove(schema);
        schema.setHardwareEligible(false);
        // Any schemas that import this schema must be hardware-disabled now
        for (CompiledSchema export : schema.getExports())
            if (export != null) hardwareDisable(export);
    }

    private static ThreadLocal<Pattern[]> tlUrlWhitelist = new ThreadLocal<Pattern[]>();

    Pattern[] getThreadLocalUrlWhitelist() {
        Pattern[] cur = tlUrlWhitelist.get();
        return cur == null ? new Pattern[0] : cur;
    }

    void setThreadLocalUrlWhitelist(Pattern[] urlWhitelist) {
        tlUrlWhitelist.set(urlWhitelist);
    }
}

