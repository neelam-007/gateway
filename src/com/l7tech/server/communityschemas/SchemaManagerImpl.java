/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.communityschemas;

import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.ReaderPreferenceReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.Sync;
import com.l7tech.common.http.GenericHttpResponse;
import com.l7tech.common.http.cache.HttpObjectCache;
import com.l7tech.common.util.CausedIOException;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.xml.SoftwareFallbackException;
import com.l7tech.common.xml.TarariLoader;
import com.l7tech.common.xml.schema.SchemaEntry;
import com.l7tech.common.xml.tarari.TarariSchemaHandler;
import com.l7tech.common.xml.tarari.TarariSchemaResolver;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.util.HttpClientFactory;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.impl.xb.xsdschema.SchemaDocument;
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
    /** Shared cache for all schema resources, system-wide. */
    private final HttpObjectCache httpObjectCache = new HttpObjectCache(maxCacheEntries, maxCacheAge);

    private final ReadWriteLock cacheLock = new ReaderPreferenceReadWriteLock();
    private final Map<String, WeakReference<CompiledSchema>> compiledSchemaCache = new WeakHashMap<String, WeakReference<CompiledSchema>>();
    private final Map<String, Map<CompiledSchema, Object>> tnsCache = new WeakHashMap<String, Map<CompiledSchema, Object>>();
    private final HttpClientFactory httpClientFactory;

    private SchemaEntryManager schemaEntryManager;

    public SchemaManagerImpl(HttpClientFactory httpClientFactory) {
        this.httpClientFactory = httpClientFactory;
    }

    public void setSchemaEntryManager(SchemaEntryManager schemaEntryManager) {
        this.schemaEntryManager = schemaEntryManager;
    }

    private final TarariSchemaHandler tarariSchemaHandler = TarariLoader.getSchemaHandler();

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
            maybeHardwareEnable(newSchema);
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

            CompiledSchema newSchema = new CompiledSchema(tns, systemId, schemadoc, softwareSchema, this, directImports);
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
                    maybeHardwareEnable(survivingSchema);
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
    private void maybeHardwareEnable(CompiledSchema schema) {
        // Short-circuit if we know we have no work to do
        if (tarariSchemaHandler == null) return;
        if (!schema.isUniqueTns()) return;
        if (schema.isRejectedByTarari()) return;
        if (schema.isLoaded()) return;

        // Check that all children are loaded
        Set<SchemaHandle> imports = schema.getImports();
        for (SchemaHandle directImport : imports) {
            if (!directImport.getCompiledSchema().isLoaded()) {
                logger.log(Level.FINE, "Unable to load hardware for schema {0} because at least one of its imports is not hardware loaded", schema);
                return;
            }
        }

        // Do the actual hardware load for this schema
        String tns = schema.getTargetNamespace();
        try {
            tarariSchemaHandler.setTarariSchemaResolver(TARARI_SCHEMA_RESOLVER);
            tarariSchemaHandler.loadHardware(schema.getSystemId(), schema.getSchemaDocument());
            logger.log(Level.INFO, "Enabled hardware acceleration for schema with targetNamespace {0}", tns);
            schema.setLoaded(true);
            schema.setRejectedByTarari(false);

            // Let all our parents know that they might be hardware-enableable
            for (CompiledSchema export : schema.getExports())
                maybeHardwareEnable(export);
        } catch (SoftwareFallbackException e) {
            schema.setLoaded(false);
            schema.setRejectedByTarari(true);
            logger.log(Level.WARNING, "Hardware acceleration unavailable for schema with targetNamespace " + tns, e);
            return;
        } catch (SAXException e) {
            schema.setLoaded(false);
            schema.setRejectedByTarari(true);
            logger.log(Level.WARNING, "Hardware acceleration unavailable for malformed schema with targetNamespace " + tns, e);
            return;
        }
    }

    /**
     * Caller must hold write lock
     */
    private void hardwareDisable(CompiledSchema schema) {
        if (tarariSchemaHandler == null) return;
        if (schema.isRejectedByTarari()) return;
        if (!schema.isLoaded()) return;

        String tns = schema.getTargetNamespace();
        logger.log(Level.INFO, "Disabling hardware acceleration for schema with tns {0}", tns);

        schema.setLoaded(false);
        try {
            tarariSchemaHandler.unloadHardware(tns);
        } finally {
            // Any schemas that import this schema must be hardware-disabled now
            for (CompiledSchema export : schema.getExports())
                hardwareDisable(export);
        }
    }



    //- PRIVATE


    private final CachingLSResourceResolver.SchemaFinder SCHEMA_FINDER = new CachingLSResourceResolver.SchemaFinder() {
        public SchemaHandle getSchema(String namespaceURI, String systemId, String baseURI) {
            SchemaEntry resolved = schemaEntryManager.getSchemaEntryFromSystemId(systemId);
            if (resolved == null) try {
                Collection<SchemaEntry> entries = schemaEntryManager.findByTNS(namespaceURI);
                if (entries == null || entries.size() < 1) return null;
                if (entries.size() > 1) {
                    logger.log(Level.WARNING, "Multiple global schemas found with target namespace: {0}", namespaceURI);
                    return null;
                }
                resolved = entries.iterator().next();
            } catch (FindException e) {
                logger.log(Level.WARNING, "Unable to resolve global schema: " + ExceptionUtils.getMessage(e), e);
                return null;
            }
            if (resolved == null) return null;  // didn't find it

            SchemaHandle handle = schemaEntryManager.getCachedSchemaHandle(resolved.getOid());
            if (handle == null) {
                // TODO do the work to fault in the apparently-new schema now
                logger.log(Level.WARNING, "Unable to import newly added schema with oid {0}: it is not yet ready to use",  resolved.getOid());
                return null;
            }

            return handle.getCompiledSchema().ref();
        }
    };

    private final TarariSchemaResolver TARARI_SCHEMA_RESOLVER = new TarariSchemaResolver() {
        public byte[] resolveSchema(String tns, String location, String baseURI) {
            final Map<CompiledSchema, Object> got = tnsCache.get(tns);
            if (got == null)
                throw new IllegalStateException("Internal error: CompiledSchema marked as present but could not find it by TNS: " + tns);
            CompiledSchema[] allgot = got.keySet().toArray(new CompiledSchema[0]);

            if (allgot.length != 1)
                throw new IllegalStateException("Internal error: CompiledSchema marked as unique TNS, but found " +
                        allgot.length + " schemas present with the target namespace " + tns);

            try {
                CompiledSchema cs = allgot[0];
                return cs.getSchemaDocument().getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e); // can't happen
            }
        }
    };


}

