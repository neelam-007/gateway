/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.communityschemas;

import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.Sync;
import EDU.oswego.cs.dl.util.concurrent.WriterPreferenceReadWriteLock;
import com.l7tech.common.http.cache.HttpObjectCache;
import com.l7tech.common.http.cache.AbstractHttpObjectCache;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.util.WhirlycacheFactory;
import com.l7tech.common.util.CausedIOException;
import com.l7tech.common.xml.TarariLoader;
import com.l7tech.common.xml.tarari.TarariSchemaHandler;
import com.l7tech.common.xml.tarari.TarariSchemaSource;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.util.HttpClientFactory;
import com.whirlycott.cache.Cache;
import com.whirlycott.cache.CacheMaintenancePolicy;
import com.whirlycott.cache.Item;
import com.whirlycott.cache.policy.LFUMaintenancePolicy;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.impl.xb.xsdschema.SchemaDocument;
import org.w3c.dom.Element;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.SAXException;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Creates and manages all in-memory instances of {@link CompiledSchema}, including uploading them to hardware
 * when they are unambiguous, and unloading them from hardware when they become ambiguous.
 * <p/>
 * Users of {@link CompiledSchema}s should feel free to hold hard references to any that are
 * actually in use, but should not cache them any longer than needed; hardware acceleration
 * relies on their prompt finalization.
 */
public class SchemaManagerImpl implements SchemaManager {
    private static final Logger logger = Logger.getLogger(SchemaManagerImpl.class.getName());

    private final int maxCacheAge = ServerConfig.getInstance().getIntProperty(ServerConfig.PARAM_SCHEMA_CACHE_MAX_AGE, 300000);
    private final int maxCacheEntries = ServerConfig.getInstance().getIntProperty(ServerConfig.PARAM_SCHEMA_CACHE_MAX_ENTRIES, 100);
    private final int hardwareRecompileLatency = ServerConfig.getInstance().getIntProperty(ServerConfig.PARAM_SCHEMA_CACHE_HARDWARE_RECOMPILE_LATENCY, 10000);
    private final int hardwareRecompileMinAge = ServerConfig.getInstance().getIntProperty(ServerConfig.PARAM_SCHEMA_CACHE_HARDWARE_RECOMPILE_MIN_AGE, 500);
    private final int hardwareRecompileMaxAge = ServerConfig.getInstance().getIntProperty(ServerConfig.PARAM_SCHEMA_CACHE_HARDWARE_RECOMPILE_MAX_AGE, 30000);

    private final ReadWriteLock cacheLock = new WriterPreferenceReadWriteLock();
    private final Map<String, WeakReference<CompiledSchema>> compiledSchemaCache = new WeakHashMap<String, WeakReference<CompiledSchema>>();
    private final Map<String, Map<CompiledSchema, Object>> tnsCache = new WeakHashMap<String, Map<CompiledSchema, Object>>();
    private final Map<CompiledSchema, Object> schemasWaitingToLoad = new WeakHashMap<CompiledSchema, Object>();

    /** Shared cache for all schema resources, system-wide.  This is the low-level cache that stores Strings, to save network calls. */
    private final HttpObjectCache<String> httpStringCache;

    /** Top-level cache for all schemas loaded from remote URLs. */
    private final HttpSchemaCache httpSchemaCache;

    private final TarariSchemaHandler tarariSchemaHandler = TarariLoader.getSchemaHandler();

    private SchemaEntryManager schemaEntryManager;
    private long lastHardwareRecompileTime = 0;
    private long lastSchemaEligibilityTime = 0;
    private long firstSchemaEligibilityTime = 0;

    private final Timer maybeRebuildHardwareCacheTimer;

    public SchemaManagerImpl(HttpClientFactory httpClientFactory) {
        if (httpClientFactory == null) throw new NullPointerException();

        HttpObjectCache.UserObjectFactory<String> userObjectFactory = new HttpObjectCache.UserObjectFactory() {
            public String createUserObject(String url, String response) {
                return response;
            }
        };

        httpStringCache = new HttpObjectCache<String>(maxCacheEntries,
                                                      maxCacheAge,
                                                      httpClientFactory,
                                                      userObjectFactory,
                                                      HttpObjectCache.WAIT_INITIAL);

        if (tarariSchemaHandler != null) {
            final TimerTask task = new TimerTask() {
                public void run() {
                    maybeRebuildHardwareCache();
                }
            };
            maybeRebuildHardwareCacheTimer = new Timer();
            maybeRebuildHardwareCacheTimer.schedule(task, 1000, hardwareRecompileLatency);
        } else {
            maybeRebuildHardwareCacheTimer = null;
        }

        httpSchemaCache = new HttpSchemaCache("HttpSchemaCache_" + System.identityHashCode(this),
                                              maxCacheEntries,
                                              1,
                                              maxCacheAge,
                                              maxCacheAge * 2); // if a URL schema hasn't been used in 2 refresh cycles, throw it out
    }

    public void setSchemaEntryManager(SchemaEntryManager schemaEntryManager) {
        this.schemaEntryManager = schemaEntryManager;
    }

    public SchemaHandle compile(String schemadoc,
                                String systemId,
                                Pattern[] urlWhitelist)
            throws ParseException
    {
        setThreadLocalUrlWhitelist(urlWhitelist);
        return doCompile(schemadoc, systemId, true, urlWhitelist);
    }

    public SchemaHandle fetchRemote(String url, Pattern[] urlWhitelist) throws IOException, ParseException {
        setThreadLocalUrlWhitelist(urlWhitelist);
        return httpSchemaCache.resolveUrl(url);
    }

    /**
     * Get the {@link CompiledSchema} for the specified schema document, reusing an existing instance
     * if possible.  If the schema was loaded from a URL, that URL should be supplied as a
     * System ID, so that imports using relative URIs can be resolved.
     *
     * @param   schemadoc the XML Schema Document to get a CompiledSchema for. Must not be null.
     * @param   systemId  the System ID from which the document was loaded. May be null or empty.
     * @param enableHardware if true, we'll call {@link #maybeEnableHardwareForNewSchema(CompiledSchema)} before
     *                      relinquishing the write lock
     * @return  a SchemaHandle for the given document.  Never null.
     * @throws  ParseException if the schema or a dependent could not be compiled
     */
    private SchemaHandle doCompile(String schemadoc,
                                   String systemId,
                                   boolean enableHardware,
                                   Pattern[] urlWhitelist)
            throws ParseException
    {
        logger.log(Level.FINE, "Compiling schema with systemId \"{0}\"", systemId);
        schemadoc = schemadoc.intern();
        Sync read = null;
        Sync write = null;
        try {
            (read = cacheLock.readLock()).acquire();
            CompiledSchema existingSchema;

            existingSchema = getCachedCompiledSchema(schemadoc);
            if (existingSchema != null) {
                logger.fine("Found cached CompiledSchema");
                return existingSchema.ref();
            }
            read.release(); read = null;

            // Compile outside of lock to avoid holding it during database activity
            CompiledSchema newSchema = compileNoCache(systemId, schemadoc, urlWhitelist);

            // Check again under the write lock to make sure some other thread hasn't gotten here first
            (write = cacheLock.writeLock()).acquire();
            existingSchema = getCachedCompiledSchema(schemadoc);
            if (existingSchema != null) {
                logger.fine("Some other thread already compiled this schema");
                return existingSchema.ref();
            }

            logger.log(Level.FINE, "Caching compiled schema with targetNamespace \"{0}\", systemId \"{1}\"", new Object[] { newSchema.getTargetNamespace(), newSchema.getSystemId() });
            // We got here first, it's our job to create and cache the CompiledSchema
            compiledSchemaCache.put(schemadoc, new WeakReference<CompiledSchema>(newSchema));

            if (enableHardware)
                maybeEnableHardwareForNewSchema(newSchema);

            return newSchema.ref();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted waiting for cache lock");
        } catch (SAXException e) {
            throw (ParseException)new ParseException("Schema is not well-formed: " + ExceptionUtils.getMessage(e), 0).initCause(e);
        } finally {
            if (write != null) write.release();
            if (read != null) read.release();
        }
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
        // Try to enable all children bottom-up
        for (SchemaHandle child : newSchema.getImports())
            maybeEnableHardwareForNewSchema(child.getCompiledSchema());

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
            maybeHardwareEnable(newSchema, false);
        } else if (compiledSchemasWithThisTns.isEmpty()) {
            logger.log(Level.FINE, "No more schemas with targetNamespace {0}", tns);
            tnsCache.remove(tns);
        }
    }

    Sync getReadLock() {
        return cacheLock.readLock();
    }

    private CompiledSchema getCachedCompiledSchema(String schemadoc) {
        WeakReference<CompiledSchema> existingSchemaRef;
        existingSchemaRef = compiledSchemaCache.get(schemadoc);
        if (existingSchemaRef == null) return null;
        return existingSchemaRef.get();
    }

    private CompiledSchema compileNoCache(String systemId,
                                          String schemadoc,
                                          final Pattern[] urlWhitelist) throws SAXException {
        // Parse software schema
        SchemaFactory sf = SchemaFactory.newInstance(XmlUtil.W3C_XML_SCHEMA);

        final LSResourceResolver lsrr;

        final Set<SchemaHandle> directImports = new HashSet<SchemaHandle>();
        CachingLSResourceResolver.ImportListener importListener = new CachingLSResourceResolver.ImportListener() {
            public void foundImport(SchemaHandle imported) {
                directImports.add(imported);
            }
        };

        CachingLSResourceResolver.SchemaCompiler schemaCompiler = new CachingLSResourceResolver.SchemaCompiler() {
            public SchemaHandle getSchema(String url, String response) throws ParseException {
                return doCompile(response, url, false, urlWhitelist);
            }
        };

        lsrr = new CachingLSResourceResolver(SCHEMA_FINDER, httpStringCache, urlWhitelist, schemaCompiler, importListener);
        sf.setResourceResolver(lsrr);

        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(schemadoc.getBytes("UTF-8"));
            Schema softwareSchema = sf.newSchema(new StreamSource(bais, systemId));
            bais.reset();
            SchemaDocument sdoc = SchemaDocument.Factory.parse(bais);
            String tns = sdoc.getSchema().getTargetNamespace();
            Element mangledElement = XmlUtil.normalizeNamespaces(XmlUtil.stringToDocument(schemadoc).getDocumentElement());
            String mangledDoc = XmlUtil.nodeToString(mangledElement);
            CompiledSchema newSchema = new CompiledSchema(tns, systemId, schemadoc, mangledDoc, softwareSchema, this, directImports);
            for (SchemaHandle directImport : directImports)
                directImport.getCompiledSchema().addExport(newSchema);
            return newSchema;

        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new SAXException("XMLBeans couldn't parse Schema", e);
        } catch (XmlException e) {
            throw new SAXException("XMLBeans couldn't parse Schema", e);
        }
    }

    /**
     * Removes a CompiledSchema from caches.  If the targetNamespace of the schema to be
     * removed was previously duplicated, and a single survivor is left, it will be promoted
     * to hardware-accelerated.
     */
    void closeSchema(CompiledSchema schema) {
        reportCacheContents();

        logger.log(Level.FINE, "Closing {0}", schema);
        String tns = schema.getTargetNamespace();

        try {
            cacheLock.writeLock().acquire();
            this.compiledSchemaCache.remove(schema.getSchemaDocument());
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
                    maybeHardwareEnable(survivingSchema, true);
                }
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.severe("Interrupted waiting for cache lock");
        } finally {
            cacheLock.writeLock().release();
        }
    }

    private void reportCacheContents() {
        if (logger.isLoggable(Level.FINEST)) {
            try {
                cacheLock.readLock().acquire();
                for (WeakReference<CompiledSchema> ref : compiledSchemaCache.values()) {
                    if (ref == null) continue;
                    CompiledSchema cs = ref.get();
                    if (cs == null) continue;
                    logger.finest("  In Cache: " + cs);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                cacheLock.readLock().release();
            }
        }
    }

    /**
     * Check if this schema can be hardware-enabled
     * Caller must hold write lock.
     */
    private boolean maybeHardwareEnable(CompiledSchema schema, boolean notifyParents) {
        // Short-circuit if we know we have no work to do
        if (tarariSchemaHandler == null) return false;
        if (!schema.isUniqueTns()) return false;
        if (schema.isRejectedByTarari()) return false;
        if (schema.isHardwareEligible()) return false;

        // Check that all children are loaded
        Set<SchemaHandle> imports = schema.getImports();
        for (SchemaHandle directImport : imports) {
            if (!directImport.getCompiledSchema().isHardwareEligible()) {
                logger.log(Level.FINE, "Unable to enable hardware eligibility for schema {0} because at least one of its imports is not hardware eligible", schema);
                return false;
            }
        }

        // Schedule the actual hardware load for this schema
        String tns = schema.getTargetNamespace();
        scheduleLoadHardware(schema);
        logger.log(Level.INFO, "Schema with targetNamespace {0} is eligible for hardware acceleration", tns);
        schema.setHardwareEligible(true);

        if (notifyParents) {
            // Let all our parents know that they might be hardware-enableable
            for (CompiledSchema export : schema.getExports())
                maybeHardwareEnable(export, notifyParents);
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
        scheduleOneShotRebuildCheck(hardwareRecompileMinAge);
    }

    /** Schedule a one-shot call to maybeRebuildHardwareCache(), delay ms from now. */
    private void scheduleOneShotRebuildCheck(long delay) {
        if (delay < 1) throw new IllegalArgumentException("Rebuild check delay must be positive");
        if (maybeRebuildHardwareCacheTimer != null) {
            TimerTask task = new TimerTask() {
                public void run() {
                    maybeRebuildHardwareCache();
                }
            };
            maybeRebuildHardwareCacheTimer.schedule(task, delay);
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

        final long beforeTime = System.currentTimeMillis();

        final long msSinceLastRebuild = beforeTime - lastHardwareRecompileTime;
        if (msSinceLastRebuild < hardwareRecompileLatency) {
            final long delay = hardwareRecompileLatency - msSinceLastRebuild;
            logger.log(Level.FINE,
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
            final long delay = hardwareRecompileMinAge - ageOfNewestSchema;
            logger.log(Level.FINER, "New schema just became hardware eligible -- postponing hardware schema cache rebuild for {0} ms", delay);
            scheduleOneShotRebuildCheck(delay);
            return false;
        }

        return true;
    }


    /**
     * Called periodically to see if it is time to rebuild the hardware schema cache.
     */
    private void maybeRebuildHardwareCache() {
        // Do an initial fast check to see if there is still anything to do, before getting the write lock
        try {
            cacheLock.readLock().acquire();
            if (!shouldRebuildNow()) return;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warning("Interrupted waiting for schema cache read lock");
            return;
        } finally {
            cacheLock.readLock().release();
        }

        // Grab the write lock and do one final check, then do the rebuild
        try {
            cacheLock.writeLock().acquire();
            if (!shouldRebuildNow()) return;

            // Do the actual
            doRebuild();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warning("Interrupted waiting for schema cache write lock");
            return;  // don't throw back up to Timer thread
        } finally {
            cacheLock.writeLock().release();
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

        for (WeakReference<CompiledSchema> ref : compiledSchemaCache.values()) {
            CompiledSchema schema = ref.get();
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
        Set<SchemaHandle> imports = schema.getImports();
        for (SchemaHandle imp : imports)
            putAllHardwareEligibleSchemasDepthFirst(alreadyAdded, schemas, imp.getCompiledSchema());

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

        schema.setHardwareEligible(false);
        // Any schemas that import this schema must be hardware-disabled now
        for (CompiledSchema export : schema.getExports())
            hardwareDisable(export);
    }

    private final CachingLSResourceResolver.SchemaFinder SCHEMA_FINDER = new CachingLSResourceResolver.SchemaFinder() {
        public SchemaHandle getSchema(String namespaceURI, String systemId, String baseURI) {
            SchemaHandle handle;
            handle = schemaEntryManager.getCachedSchemaHandleByTns(namespaceURI);
            if (handle == null) return null;
            return handle.getCompiledSchema().ref();
        }
    };

    private class HttpSchemaCache extends AbstractHttpObjectCache<SchemaHandle> {
        private final Cache cache;

        private final Lock lock = new ReentrantLock();
        protected Lock getReadLock() { return lock; }
        protected Lock getWriteLock() { return lock; }

        public HttpSchemaCache(String cacheName,
                               int maxSchemas,
                               int tunerIntervalMin,
                               long maxMillisWithNoPoll,
                               final long maxAgeUnused)
        {
            super(maxMillisWithNoPoll, WAIT_INITIAL);

            CacheMaintenancePolicy maintenancePolicy = new LFUMaintenancePolicy() {
                public void performMaintenance() {
                    final long now = System.currentTimeMillis();
                    final boolean loggit = logger.isLoggable(Level.INFO);

                    // First eliminate any schemas that have sat around for too long without being used
                    //noinspection unchecked
                    List<Map.Entry> entries = new ArrayList(new ConcurrentHashMap(managedCache).entrySet());
                    for (Map.Entry entry : entries) {
                        Item item = (Item)entry.getValue();
                        if (item != null) {
                            AbstractCacheEntry<SchemaHandle> ce = (AbstractCacheEntry<SchemaHandle>)item.getItem();
                            if (ce != null) {
                                final SchemaHandle schemaHandle = ce.getUserObject();
                                if (schemaHandle != null) {
                                    final CompiledSchema compiledSchema = schemaHandle.getCompiledSchema();
                                    if (compiledSchema.getLastUsedTime() - now > maxAgeUnused) {
                                        if (loggit) logger.info("Expiring not-recently-used remote schema loaded from URL " + compiledSchema.getSystemId());
                                        managedCache.remove(entry.getKey());
                                        schemaHandle.close();
                                    }
                                }
                            }
                        }
                    }

                    // Now do LFU maintenance on whatever's left
                    super.performMaintenance();
                }
            };

            this.cache = WhirlycacheFactory.createCache(cacheName, maxSchemas, tunerIntervalMin, maintenancePolicy);
        }

        protected AbstractCacheEntry<SchemaHandle> cacheGet(String url) {
            return (AbstractCacheEntry<SchemaHandle>)cache.retrieve(url);
        }

        protected void cachePut(String url, AbstractCacheEntry<SchemaHandle> cacheEntry) {
            cache.store(url, cacheEntry);
        }

        protected AbstractCacheEntry<SchemaHandle> cacheRemove(String url) {
            return (AbstractCacheEntry<SchemaHandle>)cache.remove(url);
        }

        protected DatedUserObject<SchemaHandle> doHttpGet(String urlStr,
                                                          String lastModifiedStr,
                                                          long lastSuccessfulPollStarted)
                throws IOException
        {
            try {
                String schemaString = httpStringCache.resolveUrl(urlStr);
                SchemaHandle schemaHandle = compile(schemaString, urlStr, getThreadLocalUrlWhitelist());
                return new DatedUserObject<SchemaHandle>(schemaHandle, null);
            } catch (ParseException e) {
                throw new CausedIOException(e);
            }
        }
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

