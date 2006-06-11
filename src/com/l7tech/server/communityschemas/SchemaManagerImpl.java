/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.communityschemas;

import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.Sync;
import EDU.oswego.cs.dl.util.concurrent.WriterPreferenceReadWriteLock;
import com.l7tech.common.http.GenericHttpResponse;
import com.l7tech.common.http.cache.HttpObjectCache;
import com.l7tech.common.util.CausedIOException;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.util.Background;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.xml.TarariLoader;
import com.l7tech.common.xml.tarari.TarariSchemaHandler;
import com.l7tech.common.xml.tarari.TarariSchemaSource;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.util.HttpClientFactory;
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
import java.util.*;
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
    private final int hardwareRecompileLatency = ServerConfig.getInstance().getIntProperty(ServerConfig.PARAM_SCHEMA_CACHE_HARDWARE_RECOMPILE_LATENCY, 20000);

    private final ReadWriteLock cacheLock = new WriterPreferenceReadWriteLock();
    private final Map<String, WeakReference<CompiledSchema>> compiledSchemaCache = new WeakHashMap<String, WeakReference<CompiledSchema>>();
    private final Map<String, Map<CompiledSchema, Object>> tnsCache = new WeakHashMap<String, Map<CompiledSchema, Object>>();
    private final Map<CompiledSchema, Object> schemasWaitingToLoad = new WeakHashMap<CompiledSchema, Object>();
    private final HttpClientFactory httpClientFactory;

    /** Shared cache for all schema resources, system-wide. */
    private final HttpObjectCache httpObjectCache = new HttpObjectCache(maxCacheEntries, maxCacheAge);
    private final TarariSchemaHandler tarariSchemaHandler = TarariLoader.getSchemaHandler();

    private SchemaEntryManager schemaEntryManager;

    public SchemaManagerImpl(HttpClientFactory httpClientFactory) {
        this.httpClientFactory = httpClientFactory;
    }

    public void setSchemaEntryManager(SchemaEntryManager schemaEntryManager) {
        this.schemaEntryManager = schemaEntryManager;
    }

    public SchemaHandle compile(String schemadoc,
                                String systemId,
                                Pattern[] urlWhitelist)
            throws InvalidDocumentFormatException
    {
        return doCompile(schemadoc, systemId, true, urlWhitelist);
    }

    public HttpObjectCache getHttpObjectCache() {
        return httpObjectCache;
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
     * @throws  com.l7tech.common.xml.InvalidDocumentFormatException if the schema or a dependent could not be compiled
     */
    private SchemaHandle doCompile(String schemadoc,
                                   String systemId,
                                   boolean enableHardware,
                                   Pattern[] urlWhitelist)
            throws InvalidDocumentFormatException
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
            throw new InvalidDocumentFormatException(e);
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
            public SchemaHandle getSchema(String url, GenericHttpResponse response) throws IOException {
                try {
                    return doCompile(response.getAsString(), url, false, urlWhitelist);
                } catch (InvalidDocumentFormatException e) {
                    throw new CausedIOException("Remote schema object was invalid: " + ExceptionUtils.getMessage(e), e);
                }
            }
        };

        lsrr = new CachingLSResourceResolver(SCHEMA_FINDER, httpClientFactory.createHttpClient(),
                                             httpObjectCache, urlWhitelist, schemaCompiler, importListener);
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
    private void maybeHardwareEnable(CompiledSchema schema, boolean notifyParents) {
        // Short-circuit if we know we have no work to do
        if (tarariSchemaHandler == null) return;
        if (!schema.isUniqueTns()) return;
        if (schema.isRejectedByTarari()) return;
        if (schema.isHardwareEligible()) return;

        // Check that all children are loaded
        Set<SchemaHandle> imports = schema.getImports();
        for (SchemaHandle directImport : imports) {
            if (!directImport.getCompiledSchema().isHardwareEligible()) {
                logger.log(Level.FINE, "Unable to enable hardware eligibility for schema {0} because at least one of its imports is not hardware eligible", schema);
                return;
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
    }

    private void scheduleLoadHardware(CompiledSchema schema) {
        schemasWaitingToLoad.put(schema, null);
        TimerTask task = new TimerTask() {
            public void run() {
                rebuildHardwareCache();
            }
        };
        Background.scheduleOneShot(task, hardwareRecompileLatency);
    }

    private void rebuildHardwareCache() {
        // Do an initial fast check to see if there is still anything to do, before getting the write lock
        try {
            cacheLock.readLock().acquire();
            if (schemasWaitingToLoad.size() < 1)
                return;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warning("Interrupted waiting for schema cache read lock");
            return;
        } finally {
            cacheLock.readLock().release();
        }

        try {
            cacheLock.writeLock().acquire();
            if (schemasWaitingToLoad.size() < 1)
                return;

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

            Map<? extends TarariSchemaSource,Exception> failedSchemas =
                    tarariSchemaHandler.setHardwareSchemas(forHardware);

            for (Map.Entry<? extends TarariSchemaSource,Exception> entry : failedSchemas.entrySet()) {
                CompiledSchema schema = (CompiledSchema)entry.getKey();
                Exception error = entry.getValue();
                if (logger.isLoggable(Level.WARNING))
                    logger.log(Level.WARNING, "Schema for target namespace \"" + schema.getTargetNamespace() + "\" cannot be hardware accelerated: " + ExceptionUtils.getMessage(error));
                hardwareDisable(schema);
            }

            schemasWaitingToLoad.clear();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warning("Interrupted waiting for schema cache write lock");
            return;
        } finally {
            cacheLock.writeLock().release();
        }
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
}

