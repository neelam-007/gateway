/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.communityschemas;

import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.ReaderPreferenceReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.Sync;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.xml.TarariLoader;
import com.l7tech.common.xml.tarari.TarariSchemaHandler;
import static com.l7tech.server.communityschemas.CompiledSchema.HardwareStatus.*;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.impl.xb.xsdschema.SchemaDocument;
import org.xml.sax.SAXException;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.lang.ref.WeakReference;

/**
 * Creates and manages all in-memory instances of {@link CompiledSchema}, including uploading them to hardware
 * when they are unambiguous, and unloading them from hardware when they become ambiguous.
 * <p/>
 * Users of {@link CompiledSchema}s should feel free to hold hard references to any that are
 * actually in use, but should not cache them any longer than needed; hardware acceleration
 * relies on their prompt finalization.
 */
public class CompiledSchemaManager {
    private static final Logger logger = Logger.getLogger(CompiledSchemaManager.class.getName());

    private final ReadWriteLock cacheLock = new ReaderPreferenceReadWriteLock();
    private final Map<String, WeakReference<CompiledSchema>> compiledSchemaCache = new WeakHashMap<String, WeakReference<CompiledSchema>>();
    private final Map<String, Map<CompiledSchema, Object>> tnsCache = new WeakHashMap<String, Map<CompiledSchema, Object>>();

    private CommunitySchemaManager communitySchemaManager;

    public void setCommunitySchemaManager(CommunitySchemaManager communitySchemaManager) {
        this.communitySchemaManager = communitySchemaManager;
    }

    /**
     * Get the {@link CompiledSchema} for the specified schema document, reusing an existing instance
     * if possible.  If the schema was loaded from a URL, that URL should be supplied as a
     * System ID, so that imports using relative URIs can be resolved.
     * @param   schemadoc the XML Schema Document to get a CompiledSchema for. Must not be null.
     * @param   systemId  the System ID from which the document was loaded. May be null or empty.
     * @return  a CompiledSchema for the given document.
     * @throws  InvalidDocumentFormatException
     */
    public SchemaHandle compile(String schemadoc, String systemId) throws InvalidDocumentFormatException {
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
            CompiledSchema newSchema = compileNoCache(systemId, schemadoc);

            // Check again under the write lock to make sure some other thread hasn't gotten here first
            (write = cacheLock.writeLock()).acquire();
            existingSchema = getCachedCompiledSchema(schemadoc);
            if (existingSchema != null) {
                logger.fine("Some other thread already compiled this schema");
                return existingSchema.ref();
            }

            return cacheNewSchema(schemadoc, newSchema).ref();
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

    private CompiledSchema getCachedCompiledSchema(String schemadoc) {
        WeakReference<CompiledSchema> existingSchemaRef;
        existingSchemaRef = compiledSchemaCache.get(schemadoc);
        if (existingSchemaRef == null) return null;
        return existingSchemaRef.get();
    }

    private CompiledSchema compileNoCache(String systemId, String schemadoc) throws SAXException {
        // Parse software schema
        SchemaFactory sf = SchemaFactory.newInstance(XmlUtil.W3C_XML_SCHEMA);
        sf.setResourceResolver(communitySchemaManager.communityLSResourceResolver());

        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(schemadoc.getBytes("UTF-8"));
            Schema softwareSchema = sf.newSchema(new StreamSource(bais));
            bais.reset();
            SchemaDocument sdoc = SchemaDocument.Factory.parse(bais);
            String tns = sdoc.getSchema().getTargetNamespace();

            return new CompiledSchema(tns, systemId, schemadoc, softwareSchema, this);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new SAXException("XMLBeans couldn't parse Schema", e);
        } catch (XmlException e) {
            throw new SAXException("XMLBeans couldn't parse Schema", e);
        }
    }

    private CompiledSchema cacheNewSchema(String schemadoc, CompiledSchema newSchema) throws InvalidDocumentFormatException {
        logger.log(Level.FINE, "Caching compiled schema with targetNamespace \"{0}\", systemId \"{1}\"", new Object[] { newSchema.getTargetNamespace(), newSchema.getSystemId() });
        // We got here first, it's our job to create and cache the CompiledSchema
        compiledSchemaCache.put(schemadoc, new WeakReference<CompiledSchema>(newSchema));

        String tns = newSchema.getTargetNamespace();
        if (tns == null) return newSchema;

        // The new CompiledSchema has a TNS, and may be hardware-accelerated if no other schema current has the same TNS.
        Map<CompiledSchema, Object> compiledSchemasWithThisTns = tnsCache.get(tns);
        if (compiledSchemasWithThisTns == null) {
            compiledSchemasWithThisTns = new WeakHashMap<CompiledSchema, Object>();
            tnsCache.put(tns, compiledSchemasWithThisTns);
        }

        compiledSchemasWithThisTns.put(newSchema, null);
        if (compiledSchemasWithThisTns.size() == 2) {
            // We're the first duplicate; disable hardware for all known CompiledSchemas with this tns
            logger.log(Level.INFO, "A second schema was found with targetNamespace {0}; disabling hardware acceleration for both", tns);
            for (CompiledSchema schema : compiledSchemasWithThisTns.keySet()) {
                hardwareDisable(schema);
            }
            return newSchema;
        }

        if (compiledSchemasWithThisTns.size() == 1) {
            hardwareEnable(newSchema);
        } else if (compiledSchemasWithThisTns.isEmpty()) {
            logger.fine("No more schemas with targetNamespace " + tns);
            tnsCache.remove(tns);
        }
        return newSchema;
    }

    /**
     * Removes a CompiledSchema from caches.  If the targetNamespace of the schema to be
     * removed was previously duplicated, and a single survivor is left, it will be promoted
     * to hardware-accelerated.
     */
    void closeSchema(CompiledSchema schema) {
        reportCacheContents();

        logger.info("Closing " + schema);
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
                try {
                    logger.fine("Enabling hardware acceleration for sole remaining schema with tns " + schema.getTargetNamespace());
                    Iterator<CompiledSchema> i = schemas.keySet().iterator();
                    // Unlikely to throw ConcurrentModificationException, since all finalizers use this method and take out the write lock
                    if (i.hasNext()) hardwareEnable(i.next());
                } catch (InvalidDocumentFormatException e) {
                    logger.log(Level.WARNING, "Hardware rejected schema for namespace " + tns + ", will fallback to software", e);
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
     * Caller must hold write lock
     */
    private void hardwareEnable(CompiledSchema schema) throws InvalidDocumentFormatException {
        if (schema.getHardwareStatus() != OFF) return;

        String tns = schema.getTargetNamespace();
        logger.info("Enabling hardware acceleration for schema with targetNamespace " + tns);

        TarariSchemaHandler tarariSchemaHandler = TarariLoader.getSchemaHandler();
        if (tarariSchemaHandler == null) return;

        try {
            tarariSchemaHandler.loadHardware(tns, schema.getSchemaDocument());
            schema.setHardwareStatus(ON);
        } catch (InvalidDocumentFormatException e) {
            schema.setHardwareStatus(BAD);
            throw e;
        }
    }

    /**
     * Caller must hold write lock
     */
    private void hardwareDisable(CompiledSchema schema) {
        if (schema.getHardwareStatus() != ON) return;

        String tns = schema.getTargetNamespace();
        logger.info("Disabling hardware acceleration for schema with tns " + tns);

        TarariSchemaHandler tarariSchemaHandler = TarariLoader.getSchemaHandler();
        if (tarariSchemaHandler == null) return;

        schema.setHardwareStatus(OFF);
        tarariSchemaHandler.unloadHardware(tns);
    }
}

