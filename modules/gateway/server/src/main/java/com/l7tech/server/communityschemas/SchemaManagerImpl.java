/**
 * Copyright (C) 2006-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.communityschemas;

import com.l7tech.common.http.*;
import com.l7tech.common.io.ByteLimitInputStream;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.server.url.AbstractUrlObjectCache;
import com.l7tech.server.url.HttpObjectCache;
import com.l7tech.util.*;
import com.l7tech.xml.DocumentReferenceProcessor;
import com.l7tech.xml.TarariLoader;
import com.l7tech.xml.tarari.TarariSchemaHandler;
import com.l7tech.xml.tarari.TarariSchemaSource;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.util.HttpClientFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
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

    private static class CacheConfiguration {
        private final int maxCacheAge;
        private final int maxCacheEntries;
        private final int hardwareRecompileLatency;
        private final int hardwareRecompileMinAge;
        private final int hardwareRecompileMaxAge;
        private final long maxSchemaSize;
        private final boolean softwareFallback;

        CacheConfiguration(Config config) {
            maxCacheAge = config.getIntProperty(ServerConfig.PARAM_SCHEMA_CACHE_MAX_AGE, 300000);
            maxCacheEntries = config.getIntProperty(ServerConfig.PARAM_SCHEMA_CACHE_MAX_ENTRIES, 100);
            hardwareRecompileLatency = config.getIntProperty(ServerConfig.PARAM_SCHEMA_CACHE_HARDWARE_RECOMPILE_LATENCY, 10000);
            hardwareRecompileMinAge = config.getIntProperty(ServerConfig.PARAM_SCHEMA_CACHE_HARDWARE_RECOMPILE_MIN_AGE, 500);
            hardwareRecompileMaxAge = config.getIntProperty(ServerConfig.PARAM_SCHEMA_CACHE_HARDWARE_RECOMPILE_MAX_AGE, 30000);
            maxSchemaSize = config.getLongProperty(ServerConfig.PARAM_SCHEMA_CACHE_MAX_SCHEMA_SIZE, HttpObjectCache.DEFAULT_DOWNLOAD_LIMIT);
            
            // This isn't "true".equals(...) just in case ServerConfig returns null--we want to default to true.
            softwareFallback = !("false".equals(config.getProperty(ServerConfig.PARAM_SCHEMA_SOFTWARE_FALLBACK, "true")));
        }
    }

    private final Config config;
    private final AtomicReference<CacheConfiguration> cacheConfigurationReference;
    private final GenericHttpClientFactory httpClientFactory;

    private final ReadWriteLock cacheLock = new ReentrantReadWriteLock(false);

    /**
     * The latest version of every currently-known schema, by URL.  This includes SchemaEntry and policy assertion
     * static schemas, as well as transient schemas that were loaded from the network.
     * <p/>
     * Superseded schema versions are removed from this map (and the handle from the map is closed).
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
    private final TarariSchemaHandler tarariSchemaHandler;

    private long lastHardwareRecompileTime = 0;
    private long lastSchemaEligibilityTime = 0;
    private long firstSchemaEligibilityTime = 0;

    private final Timer maintenanceTimer;
    private SafeTimerTask cacheCleanupTask;
    private SafeTimerTask hardwareReloadTask;

    private abstract static class SafeTimerTask extends TimerTask {
        @Override
        public final void run() {
            try {
                doRun();
            } catch (Throwable e) {
                logger.log(Level.SEVERE, "Unexpected exception on schema cache maintenance thread: " + ExceptionUtils.getMessage(e), e);
            }
        }

        protected abstract void doRun();
    }

    public SchemaManagerImpl( final Config config,
                              final HttpClientFactory httpClientFactory,
                              final Timer timer ) {
        this( config, httpClientFactory, timer, TarariLoader.getSchemaHandler() );
    }

    SchemaManagerImpl( final Config config,
                       final GenericHttpClientFactory httpClientFactory,
                       Timer timer,
                       final TarariSchemaHandler tarariSchemaHandler ) {
        if (config == null || httpClientFactory == null) throw new NullPointerException();
        this.config = validated(config);
        this.httpClientFactory = httpClientFactory;
        this.tarariSchemaHandler = tarariSchemaHandler;
        this.cacheConfigurationReference = new AtomicReference<CacheConfiguration>();
        this.httpStringCache = new AtomicReference<HttpObjectCache<String>>();

        if (timer == null)
            timer = new Timer("Schema cache maintenance", true);
        maintenanceTimer = timer;

        updateCacheConfiguration();
    }

    private synchronized void updateCacheConfiguration() {
        cacheConfigurationReference.set(new CacheConfiguration(config));
        final HttpObjectCache.UserObjectFactory<String> userObjectFactory = new HttpObjectCache.UserObjectFactory<String>() {
            @Override
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

        final GenericHttpClientFactory hcf = wrapHttpClientFactory(httpClientFactory, cacheConfigurationReference.get().maxSchemaSize);

        httpStringCache.set(new HttpObjectCache<String>( cacheConfigurationReference.get().maxCacheEntries,
                                                      cacheConfigurationReference.get().maxCacheAge,
                                                      hcf,
                                                      userObjectFactory,
                                                      HttpObjectCache.WAIT_LATEST,
                                                      ServerConfig.PARAM_SCHEMA_CACHE_MAX_SCHEMA_SIZE ));

        cacheCleanupTask = new SafeTimerTask() {
            @Override
            public void doRun() {
                cacheCleanup();
            }
        };
        maintenanceTimer.schedule(cacheCleanupTask, 4539, cacheConfigurationReference.get().maxCacheAge * 2 + 263);

        if (tarariSchemaHandler != null) {
            hardwareReloadTask = new SafeTimerTask() {
                @Override
                public void doRun() {
                    maybeRebuildHardwareCache(0);
                }
            };
            maintenanceTimer.schedule(hardwareReloadTask, 1000, cacheConfigurationReference.get().hardwareRecompileLatency);
        }
    }

    @Override
    public void propertyChange(final PropertyChangeEvent evt) {
        logger.info("Rebuilding schema cache due to change in cache configuration");
        try {
            updateCacheConfiguration();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error while rebuilding schema cache: " + ExceptionUtils.getMessage(e), e);
        }
    }

    private GenericHttpClientFactory wrapHttpClientFactory(final GenericHttpClientFactory httpClientFactory, final long maxResponseSize) {
        return new GenericHttpClientFactory() {
            @Override
            public GenericHttpClient createHttpClient() {
                return wrapHttpClient(httpClientFactory.createHttpClient());
            }

            @Override
            public GenericHttpClient createHttpClient(int hostConnections, int totalConnections, int connectTimeout, int timeout, Object identity) {
                return wrapHttpClient(httpClientFactory.createHttpClient(hostConnections, totalConnections, connectTimeout, timeout, identity));
            }

            private GenericHttpClient wrapHttpClient(final GenericHttpClient httpClient) {
                return new GenericHttpClient() {
                    @Override
                    public GenericHttpRequest createRequest(HttpMethod method, GenericHttpRequestParams params) throws GenericHttpException {
                        return wrapHttpRequest(httpClient.createRequest(method, params));
                    }
                };
            }

            private GenericHttpRequest wrapHttpRequest(final GenericHttpRequest request) {
                return new GenericHttpRequest() {
                    @Override
                    public void setInputStream(InputStream bodyInputStream) {
                        request.setInputStream(bodyInputStream);
                    }

                    @Override
                    public GenericHttpResponse getResponse() throws GenericHttpException {
                        return wrapHttpResponse(request.getResponse());
                    }

                    @Override
                    public void addParameter(String name, String value) throws IllegalArgumentException, IllegalStateException {
                        request.addParameter(name, value);
                    }

                    @Override
                    public void close() {
                        request.close();
                    }
                };
            }

            private GenericHttpResponse wrapHttpResponse(final GenericHttpResponse response) {
                return new GenericHttpResponse() {
                    @Override
                    public InputStream getInputStream() throws GenericHttpException {
                        return new ByteLimitInputStream(response.getInputStream(), 1024, maxResponseSize);
                    }

                    @Override
                    public void close() {
                        response.close();
                    }

                    @Override
                    public int getStatus() {
                        return response.getStatus();
                    }

                    @Override
                    public HttpHeaders getHeaders() {
                        return response.getHeaders();
                    }

                    @Override
                    public ContentTypeHeader getContentType() {
                        return response.getContentType();
                    }

                    @Override
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
    private void onUrlDownloaded(final String url) {
        final SchemaHandle old = schemasBySystemId.remove(url);
        if (old != null) {
            schemasRecentlySuperseded.put(url, 1);
            deferredCloseHandle(old);
        }
    }

    private synchronized void deferredCloseHandle( final SchemaHandle handle ) {
        handlesNeedingClosed.put(handle, null);
    }

    private void cacheCleanup() {
        logger.finer("Running periodic schema cache cleanup task");

        closeDeferred();

        // Do some cache maintenance.  We'll decide what to do while holding the read lock;
        // then, we'll grab the write lock and do it.
        final Map<String, SchemaHandle> urlsToRemove = new HashMap<String, SchemaHandle>();

        cacheLock.readLock().lock();
        try {
            final long maxCacheEntries = cacheConfigurationReference.get().maxCacheEntries + globalSchemasByUrl.size();

            // First, if the cache is too big, throw out the least-recently-used schemas until it isn't.
            long extras = schemasBySystemId.size() - maxCacheEntries;
            if (extras > 0) {
                final List<SchemaHandle> handles = new ArrayList<SchemaHandle>(schemasBySystemId.values());
                extras = maxCacheEntries - handles.size();
                // Have to double check in case one went away while we were copying out of the map
                if (extras > 0) {
                    Collections.sort(handles, new Comparator<SchemaHandle>() {
                        @Override
                        public int compare(SchemaHandle left, SchemaHandle right) {
                            final CompiledSchema leftCs = left.getTarget();
                            final CompiledSchema rightCs = right.getTarget();

                            if (leftCs == null && rightCs == null) return 0;
                            if (leftCs == null) return -1;
                            if (rightCs == null) return 1;

                            // Sort global schemas to the end so they never get thrown away, no matter how hoary they get
                            if (leftCs.isTransientSchema() && !rightCs.isTransientSchema())
                                return -1;
                            if (rightCs.isTransientSchema() && !leftCs.isTransientSchema())
                                return 1;

                            final Long leftTime = leftCs.getLastUsedTime();
                            final Long rightTime = rightCs.getLastUsedTime();
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
            final long now = System.currentTimeMillis();
            final long maxAge = cacheConfigurationReference.get().maxCacheAge * 4;
            for (final SchemaHandle schemaHandle : schemasBySystemId.values()) {
                final CompiledSchema schema = schemaHandle.getTarget();
                if (schema != null && schema.isTransientSchema()) {
                    final long lastUsed = schema.getLastUsedTime();
                    final long useAge = now - lastUsed;
                    if (useAge > maxAge) {
                        // It hasn't been used in a while.  Is it contributing to a TNS conflict?
                        if ( schema.isConflictingTns() ) {
                            // It is Part Of The Problem.  Throw it out until someone wants it again.
                            urlsToRemove.put(schema.getSystemId(), schemaHandle);
                        }
                    }
                }
            }
        } finally {
             cacheLock.readLock().unlock();
        }

        // Now remove em
        cacheLock.writeLock().lock();
        try {
            for (final Map.Entry<String, SchemaHandle> entry : urlsToRemove.entrySet()) {
                final String url = entry.getKey();
                final SchemaHandle handle = entry.getValue();

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
        for (final SchemaHandle schemaHandle : schemasBySystemId.values()) {
            final CompiledSchema schema = schemaHandle.getTarget();
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
     * Get the target namespace for the schema
     *
     * @param schema The schema
     * @return The target namespace (never null, may be empty)
     */
    private String getTargetNamespace( final CompiledSchema schema ) {
        return schema.getTargetNamespace() == null ? "" : schema.getTargetNamespace();
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
        for (final SchemaHandle handle : deferredClose)
            if (handle != null) handle.close();
    }

    /* Caller must hold at least the read lock. */
    private static void visitAllParentsRecursive( final CompiledSchema schema,
                                                  final Set<CompiledSchema> visited) {
        if (visited.contains(schema)) return;
        visited.add(schema);
        Set<CompiledSchema> exports = schema.getExports();
        for (final CompiledSchema parent : exports) {
            if (parent != null) visitAllParentsRecursive(parent, visited);
        }
    }

    /* Caller must hold at least the read lock. */
    private void getParents( final String url, final Set<CompiledSchema> collected ) {
        final Collection<SchemaHandle> allHandles = new ArrayList<SchemaHandle>(schemasBySystemId.values());
        for (final SchemaHandle schemaHandle : allHandles) {
            final CompiledSchema schema = schemaHandle.getTarget();
            if (schema == null) continue;
            final Collection<SchemaHandle> dependencies = schema.getDependencies().values();
            for (final SchemaHandle handle : dependencies) {
                final CompiledSchema depSchema = handle.getTarget();
                if (depSchema == null) continue;
                if (url.equals(depSchema.getSystemId())) {
                    visitAllParentsRecursive(depSchema, collected);
                }
            }
        }
    }

    /**
     * @return schema for URL, creating it if necessary.
     *         This is a new handle duped just for the caller; caller must close it when they are finished with it.
     */
    @Override
    public SchemaHandle getSchemaByUrl( final String url ) throws IOException, SAXException {
        cacheLock.readLock().lock();
        try {
            final SchemaHandle ret = getSchemaByUrlNoCompile(url);
            if (ret != null) return ret;
        } finally {
            cacheLock.readLock().unlock();
        }

        // Cache miss.  We'll need to compile a new instance of this schema.
        final String schemaDoc = getSchemaStringForUrl(url, url, true).getStringData();
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
            final SchemaHandle ret = getSchemaByUrlNoCompile(url);
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
    private SchemaHandle getSchemaByUrlNoCompile( final String url ) {
        final SchemaHandle handle = schemasBySystemId.get(url);
        if (handle != null) {
            final CompiledSchema schema = handle.getTarget();
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
     * @return as LSInput that contains both StringData and a SystemId.  Never null.
     * @throws IOException  if schema text could not be fetched for the specified URL.
     */
    private LSInput getSchemaStringForUrl( final String baseUrl,
                                           String url,
                                           final boolean policyOk) throws IOException {
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
        final AbstractUrlObjectCache.FetchResult<String> result =
                httpStringCache.get().fetchCached(url, HttpObjectCache.WAIT_LATEST);

        schemaDoc = result.getUserObject();
        if (schemaDoc != null)
            return makeLsInput(url, schemaDoc);

        // Check for errors we should report
        final IOException e = result.getException();
        if (e != null)
            throw new CausedIOException("Unable to download remote schema " + describeResource(baseUrl, url) +  " : " + ExceptionUtils.getMessage(e), e);

        // Shouldn't happen
        throw new IOException("Unable to download remote schema " + describeResource(baseUrl, url));
    }

    /** @return an LSInput that contains StringData and a SystemId. */
    private LSInput makeLsInput(final String url, final String schemaDoc) {
        final LSInputImpl lsi =  new LSInputImpl();
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
    private void maybeEnableHardwareForNewSchema(final CompiledSchema newSchema) {
        maybeEnableHardwareForNewSchema( newSchema, new HashSet<CompiledSchema>() );
    }

    /**
     * Add this schema to the TNS cache, and enable hardware if this is the only schema using it and
     * only if all child schemas are already hardware-enabled.
     * <p/>
     * Caller must hold the write lock.
     *
     * @param newSchema the schema for which to maybe enable hardware.
     * @param processed schemas already processed
     */
    private void maybeEnableHardwareForNewSchema( final CompiledSchema newSchema,
                                                  final Set<CompiledSchema> processed ) {
        if (newSchema == null) return;
        // Try to enable all children bottom-up
        for (final SchemaHandle child : newSchema.getDependencies().values())
            maybeEnableHardwareForNewSchema(child.getTarget(), processed);

        if ( !processed.contains( newSchema ) ) {
            final String tns = getTargetNamespace(newSchema);

            // The new CompiledSchema has a TNS, and may be hardware-accelerated if no other schema current has the same TNS.
            Map<CompiledSchema, Object> compiledSchemasWithThisTns = tnsCache.get(tns);
            if (compiledSchemasWithThisTns == null) {
                compiledSchemasWithThisTns = new WeakHashMap<CompiledSchema, Object>();
                tnsCache.put(tns, compiledSchemasWithThisTns);
            }

            boolean wasConflicting = conflictingTns(compiledSchemasWithThisTns.keySet());
            compiledSchemasWithThisTns.put(newSchema, null);
            if ( conflictingTns(compiledSchemasWithThisTns.keySet()) ) {
                // We're the first duplicate; disable hardware for all known CompiledSchemas with this tns
                if ( !wasConflicting ) {
                    logger.log(Level.INFO, "Multiple schema found for targetNamespace {0}; none are now eligible for hardware acceleration", tns);
                }

                for (CompiledSchema schema : compiledSchemasWithThisTns.keySet()) {
                    if ( !schema.isConflictingTns() ) {
                        schema.setConflictingTns(true);
                        hardwareDisable(schema);
                    }
                }
            } else if ( !compiledSchemasWithThisTns.isEmpty() ) {
                newSchema.setConflictingTns(false);
                maybeHardwareEnable(newSchema, false, false, new HashSet<CompiledSchema>());
            } else {
                logger.log(Level.FINE, "No more schemas with targetNamespace {0}", tns);
                tnsCache.remove(tns);
            }

            processed.add( newSchema );
        }
    }

    /**
     * A target namespace conflicts if there are multiple non-dependency schemas using it.
     */
    private boolean conflictingTns( final Collection<CompiledSchema> schemas ) {
        boolean duplicate = false;

        if ( schemas.size() > 1 ) {
            int count = 0;
            for ( final CompiledSchema schema : schemas ) {
                if ( !schema.isInclude() && ++count > 1 ) {
                    break;
                }
            }

            duplicate = count > 1;
        }

        return duplicate;
    }

    Lock getReadLock() {
        return cacheLock.readLock();
    }

    @Override
    public void registerSchema( final String globalUrl,
                                final String schemaDoc ) {
        if (schemaDoc == null) throw new NullPointerException("A schema must be provided");
        String old = globalSchemasByUrl.put(globalUrl, schemaDoc);
        if (old != null)
            onUrlDownloaded(globalUrl);
    }

    @Override
    public void unregisterSchema(final String globalUrl) {
        String old = globalSchemasByUrl.remove(globalUrl);
        if (old != null)
            onUrlDownloaded(globalUrl);
    }

    private String describeResource( final String baseURI, final String url ) {
        return describeResource(baseURI, url, null, null);
    }

    private String describeResource( final String baseURI,
                                     final String systemId,
                                     final String publicId,
                                     final String namespaceURI ) {
        final String description;

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

    private static class UnresolvableException extends RuntimeException {
        private final String resourceDescription;

        private UnresolvableException(Throwable cause, String resourceDescription) {
            super(cause);
            this.resourceDescription = resourceDescription;
        }

        @Override
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
     * all child and grandchild (etc) schema strings will be hot in the httpStringCache.
     *
     * @param systemId The system identifier for the schema document
     * @param schemaDoc The schema document
     * @return the dependencies directly or indirectly used by this schema.  Never null.
     * @throws SAXException if a schema is not valid
     * @throws IOException if a remote schema cannot be fetched
     */
    private Set<String> preCacheSchemaDependencies( final String systemId,
                                                    final String schemaDoc ) throws SAXException, IOException {
        final Set<String> dependencies = new HashSet<String>();

        SchemaFactory sf = SchemaFactory.newInstance(XmlUtil.W3C_XML_SCHEMA);
        LSResourceResolver resolver = new LSResourceResolver() {
            @Override
            public LSInput resolveResource(String type,
                                           String namespaceURI,
                                           String publicId,
                                           String systemId,
                                           String baseURI)
            {
                try {
                    if (systemId == null) {
                        String resolvedSystemId = generateURN(namespaceURI);
                        if (globalSchemasByUrl.containsKey(resolvedSystemId)) {
                            systemId = resolvedSystemId;
                        } else if (namespaceURI != null && globalSchemasByUrl.containsKey(namespaceURI)) {
                            systemId = namespaceURI;
                        } else {
                            throw new CausedIOException("No systemId, cannot resolve resource");
                        }
                    }
                    LSInput lsi = getSchemaStringForUrl(baseURI, systemId, false);
                    assert lsi != null;
                    dependencies.add(lsi.getSystemId());
                    return lsi;
                } catch (IOException e) {
                    throw new UnresolvableException(e, describeResource(baseURI, systemId, publicId, namespaceURI));
                }
            }
        };
        sf.setResourceResolver(resolver);

        try {
            sf.newSchema(new StreamSource(new StringReader(schemaDoc), systemId)); // populates dependencies as side-effect
            return dependencies;
        } catch (RuntimeException e) {
            UnresolvableException exception = ExceptionUtils.getCauseIfCausedBy(e, UnresolvableException.class);
            if (exception != null) throw new CausedIOException("Unable to resolve remote sub-schema for resource " + exception.getResourceDescription(), exception.getCause());
            throw e;
        }
    }

    private String generateURN( final String namespace ) {
        StringBuilder sb = new StringBuilder();
        sb.append("urn:uuid:");
        byte[] bytes = namespace==null ? new byte[0] : namespace.getBytes(Charsets.UTF8);
        sb.append(UUID.nameUUIDFromBytes(bytes).toString());
        return sb.toString();
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
    private String computeEffectiveUrl( final String base, final String relative ) throws MalformedURLException {
        if (base == null || relative == null) throw new NullPointerException();

        // First check if the "relative" uri is in fact absolute, in which case we avoid
        // parsing the base as a URL in case it is relative.
        try {
            final URI relativeUri = new URI(relative);
            if (relativeUri.isAbsolute()) {
                final String scheme = relativeUri.getScheme();
                if (!scheme.equals("http") && !scheme.equals("https"))
                    throw new MalformedURLException("Refusing remote schema reference with non-HTTP(S) base URL: " + base);
                return relativeUri.toURL().toExternalForm();
            }
        }
        catch(URISyntaxException use) {
            // we'll find out below ...
        }

        final URL baseUrl = new URL(base);

        final String protocol = baseUrl.getProtocol();
        if (!protocol.equals("http") && !protocol.equals("https"))
            throw new MalformedURLException("Refusing remote schema reference with non-HTTP(S) base URL: " + base);

        return new URL(baseUrl, relative).toExternalForm();
    }

    /**
     * Compile this schema document.  This may require fetching remote schemas.
     * Caller must hold the write lock.
     *
     * @param systemId
     * @param schemaDoc
     * @return a new handle, duped just for the caller.  Caller must close it when they are finished with it.
     * @throws SAXException
     */
    private SchemaHandle compileAndCache( final String systemId, final String schemaDoc )
            throws SAXException, IOException
    {
        // Do initial parse and get dependencies (strings are all hot in the HTTP cache after this)
        preCacheSchemaDependencies(systemId, schemaDoc);

        invalidateParentsOfRecentlySupersededSchemas();

        SchemaHandle schemaHandle = compileAndCacheRecursive(systemId, schemaDoc, new HashSet<String>());
        maybeEnableHardwareForNewSchema(schemaHandle.getTarget());
        return schemaHandle;
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
     * This will recursively compile the schemas dependencies.
     * <p/>
     * Caller must hold the write lock.
     *
     * @param systemId    systemId of the schema being compiled. required
     * @param schemaDoc   a String containing the schema XML
     * @param seenSystemIds  the set of system IDs seen since the current top-level compile began
     * @return a SchemaHandle to a new CompiledSchema instance, already duplicated for the caller.  Caller must close
     *         this handle when they are finished with it.
     */
    private SchemaHandle compileAndCacheRecursive( final String systemId,
                                                   final String schemaDoc,
                                                   final Set<String> seenSystemIds ) throws SAXException, IOException {
        if (seenSystemIds.contains(systemId)) {
            logger.info("Circular import detected.");
            return null;
        }

        seenSystemIds.add(systemId);

        // Re-parse, building up CompiledSchema instances as needed from the bottom up
        final SchemaFactory sf = SchemaFactory.newInstance(XmlUtil.W3C_XML_SCHEMA);

        final Document schema = XmlUtil.stringToDocument(schemaDoc);
        final Set<String> includes = new HashSet<String>();
        final DocumentReferenceProcessor schemaReferenceProcessor = DocumentReferenceProcessor.schemaProcessor();
        schemaReferenceProcessor.processDocumentReferences( schema, new DocumentReferenceProcessor.ReferenceCustomizer(){
            @Override
            public String customize( final Document document, 
                                     final Node node,
                                     final String documentUrl,
                                     final DocumentReferenceProcessor.ReferenceInfo referenceInfo ) {
                if ( !"import".equals(node.getLocalName()) && referenceInfo.getReferenceUrl()!=null ) {
                    includes.add( referenceInfo.getReferenceUrl() );
                }
                return null;
            }
        } );

        final Map<String,SchemaHandle> directImports = new HashMap<String,SchemaHandle>();
        final Map<String,SchemaHandle> directIncludes = new HashMap<String,SchemaHandle>();
        final LSResourceResolver resolver = new LSResourceResolver() {
            @Override
            public LSInput resolveResource( final String type,
                                            final String namespaceURI,
                                            final String publicId,
                                            final String systemId,
                                            final String baseURI) {
                if ( !XMLConstants.W3C_XML_SCHEMA_NS_URI.equals(type) ) {
                    throw new UnresolvableException(null, describeResource(baseURI, systemId, publicId, namespaceURI));
                }

                Map<String,SchemaHandle> dependencyMap = includes.contains(systemId) ? directIncludes : directImports;
                try {
                    return resolveSchema( namespaceURI, systemId, baseURI, seenSystemIds, dependencyMap );
                } catch (IOException e) {
                    throw new UnresolvableException(e, describeResource(baseURI, systemId, publicId, namespaceURI));
                } catch (SAXException e) {
                    throw new UnresolvableException(e, describeResource(baseURI, systemId, publicId, namespaceURI));
                }
            }
        };
        sf.setResourceResolver(resolver);

        boolean success = false;
        try {
            final Schema softwareSchema = sf.newSchema(new StreamSource(new StringReader(schemaDoc), systemId));
            final String tns = XmlUtil.getSchemaTNS(schemaDoc);
            final Element mangledElement = DomUtils.normalizeNamespaces(schema.getDocumentElement());
            final CompiledSchema newSchema =
                    new CompiledSchema(tns, systemId, schemaDoc, mangledElement, softwareSchema, this,
                            directImports, directIncludes, true, cacheConfigurationReference.get().softwareFallback);
            for ( final SchemaHandle directDependency : CollectionUtils.iterable(directImports.values(), directIncludes.values()) ) {
                final CompiledSchema impSchema = directDependency.getCompiledSchema();
                if (impSchema == null) continue;
                impSchema.addExport(newSchema);
            }

            final SchemaHandle cacheRef = newSchema.ref(); // make a handle for the cache
            final SchemaHandle old = schemasBySystemId.put(newSchema.getSystemId(), cacheRef);
            if (old != null) deferredCloseHandle(old);
            success = true;
            return newSchema.ref(); // make a handle for the caller

        } catch (XmlUtil.BadSchemaException e) {
            throw new SAXException("Unable to parse Schema", e);
        } catch (RuntimeException e) {
            UnresolvableException exception = ExceptionUtils.getCauseIfCausedBy(e, UnresolvableException.class);
            if (exception != null) throw new CausedIOException("Unable to resolve remote sub-schema for resource " + exception.getResourceDescription(), exception.getCause());
            throw e;
        } finally {
            if ( !success ) {
                for (SchemaHandle impHandle : CollectionUtils.iterable(directImports.values(), directIncludes.values())) {
                    impHandle.close();
                }
            }
        }
    }

    private LSInput resolveSchema( final String namespaceURI,
                                   String systemId,
                                   final String baseURI,
                                   final Set<String> seenSystemIds,
                                   final Map<String, SchemaHandle> dependencyMap ) throws IOException, SAXException {
        if (systemId == null) {
            String resolvedSystemId = generateURN(namespaceURI);
            if (globalSchemasByUrl.containsKey(resolvedSystemId)) {
                systemId = resolvedSystemId;
            } else if (namespaceURI != null && globalSchemasByUrl.containsKey(namespaceURI)) {
                systemId = namespaceURI;
            } else {
                throw new CausedIOException("No systemId, cannot resolve resource");
            }
        }
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

        //if handle is null, there was a circular dependency.
        if ( handle != null ) {
            dependencyMap.put(handle.getCompiledSchema().getSystemId(), handle); // give it away without closing it
            return makeLsInput(handle.getCompiledSchema().getSystemId(), handle.getCompiledSchema().getSchemaDocument());
        }
        return lsi;
    }

    /**
     * Removes a CompiledSchema from caches.  If the targetNamespace of the schema to be
     * removed was previously duplicated, and a single survivor is left, it will be promoted
     * to hardware-accelerated.
     * <p/>
     * Caller must NOT hold the write lock.  Caller will typically be either and end user thread,
     * the cache cleanup thread, or a finalizer thread.
     */
    void closeSchema( final CompiledSchema schema ) {
        reportCacheContents();

        logger.log(Level.FINE, "Closing {0}", schema);
        final String tns = getTargetNamespace(schema);

        cacheLock.writeLock().lock();
        try {

            SchemaHandle old = schemasBySystemId.get(schema.getSystemId());
            if (old != null && old.getTarget() == schema) {
                // We were the active schema for this URL -- remove ourselves
                schemasBySystemId.remove(schema.getSystemId());
                schemasRecentlySuperseded.put(schema.getSystemId(), 1);
            }

            // The schema had a TNS, and might have been relevant to hardware
            hardwareDisable(schema, true);
            Map<CompiledSchema, Object> schemas = tnsCache.get(tns);
            if (schemas == null) return;
            schemas.remove(schema);
            if ( !conflictingTns( schemas.keySet() ) ) {
                // Survivors get hardware privileges
                if (logger.isLoggable(Level.INFO))
                    logger.log(Level.INFO, "Remaining schemas with tns \"{0}\" are now eligible for hardware acceleration", tns);
                // Unlikely to throw ConcurrentModificationException, since all finalizers use this method and take out the write lock
                for ( CompiledSchema survivingSchema : schemas.keySet() ) {
                    survivingSchema.setConflictingTns(false);
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
    private void getRoots( final CompiledSchema schema,
                           final Set<CompiledSchema> foundRoots ) {
        Set<CompiledSchema> exports = schema.getExports();
        boolean haveExport = false;
        for (CompiledSchema parent : exports) {
            if (parent == null) continue;
            haveExport = true;
            getRoots(parent, foundRoots);
        }
        if (!haveExport) foundRoots.add(schema);
    }

    private void reportNode( final CompiledSchema schema,
                             final Set<CompiledSchema> visited,
                             final StringBuilder sb,
                             final String indent) {
        visited.add(schema);
        sb.append(indent).append(schema);
        for (int i = indent.length(); i < 14; ++i) sb.append(" ");
        sb.append(schema.getTnsGen()).append("\n");
        Map<String,SchemaHandle> kids = schema.getDependencies();
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
    private boolean maybeHardwareEnable( final CompiledSchema schema,
                                         final boolean notifyParents,
                                         final boolean notifyChildren,
                                         final Set<CompiledSchema> visited ) {
        // Short-circuit if we know we have no work to do
        if (tarariSchemaHandler == null) return false;
        boolean notYetVisited = visited.add(schema);
        assert notYetVisited;
        if (schema.isConflictingTns()) return false;
        if (!schema.isTarariValid()) return false;
        if (schema.isHardwareEligible()) return false;

        // Check that all children are loaded
        Map<String,SchemaHandle> dependencies = schema.getDependencies();
        for (SchemaHandle directImport : dependencies.values()) {
            final CompiledSchema cs = directImport.getTarget();
            if (cs == null) continue;
            if (visited.contains(cs)) continue; // prevent infinite downwards and upwards recursion
            if (!cs.isHardwareEligible()) {
                if (!notifyChildren) {
                    logger.log(Level.FINE, "Unable to enable hardware eligibility for schema {0} because at least one of its dependencies is not already hardware eligible", schema);
                    return false;
                }

                if (!maybeHardwareEnable(cs, notifyParents, true, visited)) {
                    logger.log(Level.FINE, "Unable to enable hardware eligibility for schema {0} because at least one of its dependencies could not be made hardware eligible", schema);
                    return false;
                }
            }
        }

        // Schedule the actual hardware load for this schema
        scheduleLoadHardware(schema);
        logger.log(Level.INFO, "Schema with targetNamespace \"{0}\" is eligible for hardware acceleration", getTargetNamespace(schema));
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
    private void scheduleLoadHardware( final CompiledSchema schema ) {
        schemasWaitingToLoad.put(schema, null);

        final long now = System.currentTimeMillis();
        if (firstSchemaEligibilityTime == 0)
            firstSchemaEligibilityTime = now;
        lastSchemaEligibilityTime = now;
        scheduleOneShotRebuildCheck( cacheConfigurationReference.get().hardwareRecompileMinAge);
    }

    /** Schedule a one-shot call to maybeRebuildHardwareCache(), delay ms from now. */
    private void scheduleOneShotRebuildCheck( final long delay ) {
        if (delay < 1) throw new IllegalArgumentException("Rebuild check delay must be positive");
        if (maintenanceTimer != null) {
            SafeTimerTask task = new SafeTimerTask() {
                private final long scheduleTime = System.currentTimeMillis();
                @Override
                public void doRun() {
                    maybeRebuildHardwareCache(scheduleTime);
                }
            };
            maintenanceTimer.schedule(task, delay);
        }
    }

    /**
     * Check if hardware cache should be rebuilt right now.
     *
     * Caller must hold either the read lock or write lock.
     *
     * If schemasWaitingToLoad is non-empty, this method may still return false (postponing the rebuild)
     *
     * @return true if the hardware cache should be rebuilt immediately.
     *         false if the hardware cache does not need to be rebuilt at this time.  If there are schemas
     *         waiting to be rebuilt, a new schema check is scheduled occur in the near future.
     */
    boolean shouldRebuildNow( long scheduledTime ) {
        if (schemasWaitingToLoad.size() < 1 || (scheduledTime > 0 && scheduledTime < lastHardwareRecompileTime) )
            return false;

        int hardwareRecompileLatency = cacheConfigurationReference.get().hardwareRecompileLatency;
        int hardwareRecompileMinAge = cacheConfigurationReference.get().hardwareRecompileMinAge;
        int hardwareRecompileMaxAge = cacheConfigurationReference.get().hardwareRecompileMaxAge;

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
                logger.log(Level.FINER,  "Hardware eligible schema has been awaiting reload for {0} ms -- forcing hardware schema cache reload now", ageOfOldestSchema);
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
    void maybeRebuildHardwareCache( long scheduledTime ) {
        // Do an initial fast check to see if there is anything to do, before getting the concurrency-killing write lock
        cacheLock.readLock().lock();
        try {
            if (!shouldRebuildNow(scheduledTime)) return;
        } finally {
            cacheLock.readLock().unlock();
        }

        // Grab the write lock and do one final check, then do the rebuild
        cacheLock.writeLock().lock();
        try {
            if (!shouldRebuildNow(scheduledTime)) return;

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
        HashMap<String, CompiledSchema> forHardware = new HashMap<String, CompiledSchema>();
        Set<CompiledSchema> alreadyAdded = new HashSet<CompiledSchema>();

        for (SchemaHandle handle : schemasBySystemId.values()) {
            CompiledSchema schema = handle.getTarget();
            if (schema == null) continue;
            schema.setLoaded(false);
            putAllHardwareEligibleSchemasDepthFirst(alreadyAdded, forHardware, schema);
        }

        if (logger.isLoggable(Level.INFO)) logger.info("Rebuilding hardware schema cache: loading " + forHardware.size() + " eligible schemas (" + schemasWaitingToLoad.size() + " new)");

        Map<TarariSchemaSource,Exception> failedSchemas =
                tarariSchemaHandler.setHardwareSchemas(forHardware);

        lastHardwareRecompileTime = System.currentTimeMillis(); // get time again after

        if (beforeTime > 0)
            logger.log(Level.FINE, "Rebuild of hardware schema cache took {0} ms", lastHardwareRecompileTime - beforeTime);

        for (Map.Entry<? extends TarariSchemaSource,Exception> entry : failedSchemas.entrySet()) {
            CompiledSchema schema = (CompiledSchema)entry.getKey();
            Exception error = entry.getValue();
            if (logger.isLoggable(Level.WARNING))
                logger.log(Level.WARNING, "Schema for target namespace \"" + getTargetNamespace(schema) + "\" cannot be hardware accelerated: " + ExceptionUtils.getMessage(error));
            hardwareDisable(schema);
        }

        schemasWaitingToLoad.clear();
        firstSchemaEligibilityTime = 0;
    }

    /** Recursively populate linked hash map of hardware-eligible schemas ordered so that includes come before their dependents. */
    private void putAllHardwareEligibleSchemasDepthFirst( final Set<CompiledSchema> alreadyAdded,
                                                          final HashMap<String, CompiledSchema> schemas,
                                                          final CompiledSchema schema )
    {
        Map<String,SchemaHandle> dependencies = schema.getDependencies();
        for (SchemaHandle imp : dependencies.values()) {
            final CompiledSchema impSchema = imp.getTarget();
            if (impSchema == null) continue;
            putAllHardwareEligibleSchemasDepthFirst(alreadyAdded, schemas, impSchema);
        }

        if (schema.isHardwareEligible() && !alreadyAdded.contains(schema))
            schemas.put(schema.getSystemId(), schema);
    }

    /**
     * Caller must hold write lock
     */
    private void hardwareDisable( final CompiledSchema schema ) {
        hardwareDisable( schema, false );
    }

    /**
     * Caller must hold write lock
     */
    private void hardwareDisable( final CompiledSchema schema, final boolean closing ) {
        if (tarariSchemaHandler == null) return;

        if (!closing && schema.isHardwareEligible() && logger.isLoggable(Level.INFO))
            logger.log(Level.INFO, "Disabling hardware acceleration eligibility for schema {0}",
                       String.valueOf(schema.getSystemId()));

        schemasWaitingToLoad.remove(schema);
        schema.setHardwareEligible(false);
        // Any schemas that import this schema must be hardware-disabled now
        for (CompiledSchema export : schema.getExports())
            if (export != null) hardwareDisable(export, closing);
    }

    private static Config validated( final Config config ) {
        final ValidatedConfig vc = new ValidatedConfig( config, logger, new Resolver<String,String>(){
            @Override
            public String resolve( final String key ) {
                return ServerConfig.getInstance().getClusterPropertyName( key );
            }
        } );

        vc.setMinimumValue( ServerConfig.PARAM_SCHEMA_CACHE_MAX_ENTRIES, 0 );
        vc.setMaximumValue( ServerConfig.PARAM_SCHEMA_CACHE_MAX_ENTRIES, 1000000 );

        return vc;
    }
}

