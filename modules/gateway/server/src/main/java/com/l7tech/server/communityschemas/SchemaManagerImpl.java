package com.l7tech.server.communityschemas;

import com.l7tech.common.io.DocumentReferenceProcessor;
import com.l7tech.common.io.IOExceptionThrowingReader;
import com.l7tech.common.io.ResourceReference;
import com.l7tech.common.io.SchemaUtil;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.server.audit.LogOnlyAuditor;
import com.l7tech.util.*;
import com.l7tech.xml.TarariLoader;
import com.l7tech.xml.tarari.TarariSchemaHandler;
import com.l7tech.xml.tarari.TarariSchemaSource;
import static com.l7tech.server.communityschemas.SchemaSourceResolver.*;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.ext.EntityResolver2;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
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
@ManagedResource(description="XML Schema Manager", objectName="l7tech:type=XMLSchemaManager")
public class SchemaManagerImpl implements ApplicationListener, SchemaManager, SchemaSourceResolver.SchemaInvalidationListener {
    private static final Logger logger = Logger.getLogger(SchemaManagerImpl.class.getName());

    private static final long minCleanupPeriod = SyspropUtil.getLong( "com.l7tech.server.schema.minCleanupPeriod", 5000L );
    private static final boolean allowRemoteReferencesFromLocal = SyspropUtil.getBoolean( "com.l7tech.server.schema.allowRemote", false );
    private static final boolean strictDoctypeChecking = SyspropUtil.getBoolean( "com.l7tech.server.schema.strictDoctypeCheck", true );

    /**
     * Validated schema configuration.
     */
    private final SchemaConfiguration config;

    private final ReadWriteLock cacheLock = new ReentrantReadWriteLock(false);

    /**
     * The latest version of every currently-known schema, by URI.  This includes both registered schemas,
     * as well as transient schemas that were loaded from the network.
     * <p/>
     * Superseded schema versions are removed from this map (and the handle from the map is closed).
     * A superseded schema's claim to its TNS will be released when its last user handle closes'.
     * <p/>
     * For the transient schemas loaded from the network, a periodic task removes URIs whose schemas have not
     * been used recently.
     * <p/>
     * Access is not lock-protected.
     */
    private final Map<String, SchemaHandle> schemasBySystemId = new ConcurrentHashMap<String, SchemaHandle>();

    /**
     * For each currently-active TNS, stores (weakly) the set of currently-active schemas that use that TNS.
     * Both registered and remote schemas may be present in the TNS cache.
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
     * Stores the latest version of each registered schema string (Hardware schemas and policy assertion
     * static schemas). Map of URI to SchemaResource.
     */
    private final Map<String, SchemaResource> registeredSchemasByUri = new ConcurrentHashMap<String, SchemaResource>();

    /**
     * Stores the latest version of each registered schema string (Hardware schemas and policy assertion
     * static schemas). Map of Target Namespace to SchemaResources.
     */
    private final Map<String, Set<SchemaResource>> registeredSchemasByTargetNamespace = new ConcurrentHashMap<String, Set<SchemaResource>>();

    /**
     * Stores the reference count for registeredUrls.
     */
    private final Map<String, Integer> registeredUris = new HashMap<String,Integer>();

    /**
     * Stores the set of schema URIs that were superseded by new downloads since the last compile began.
     * (Currently schema compilation is restricted to a single thread at a time.)
     */
    private final Map<String, Integer> schemasRecentlySuperseded = new ConcurrentHashMap<String, Integer>();

    /**
     * Stores handles that should be closed next time someone is free to do so (holding no locks).
     * If nobody does it before then (such as the finalizer), the maintenance task will close these handles.
     * Don't use this directly; use the {@link #deferredCloseHandle(SchemaHandle)} method instead.
     */
    private final Map<SchemaHandle, Object> handlesNeedingClosed = new WeakHashMap<SchemaHandle, Object>();

    /**
     * The EntityResolver to use when parsing schema documents
     */
    private final AtomicReference<EntityResolver> entityResolverRef = new AtomicReference<EntityResolver>(XmlUtil.getSafeEntityResolver());

    /**
     * The XML parser function to use for parsing schema documents.
     */
    private final AtomicReference<SchemaSourceTransformer> schemaSourceTransformerRef = new AtomicReference<SchemaSourceTransformer>( new SafeSchemaSourceTransformer() );

    /** Bean that handles Tarari hardware reloads for us, null if no Tarari. */
    private final TarariSchemaHandler tarariSchemaHandler;

    /**
     * Sources for XML Schema documents.
     */
    private final Collection<SchemaSourceResolver> schemaSourceResolvers;

    /**
     * Entity resolver for use parsing schema documents (when doctypes are permitted)
     */
    private final EntityResolver schemaEntityResolver;

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

    public SchemaManagerImpl( final SchemaConfiguration config,
                              final Timer timer,
                              final SchemaSourceResolver[] schemaSourceResolvers,
                              final EntityResolver schemaEntityResolver ) {
        this( config, timer, TarariLoader.getSchemaHandler(), schemaSourceResolvers, schemaEntityResolver );
    }

    SchemaManagerImpl( final SchemaConfiguration config,
                       Timer timer,
                       final TarariSchemaHandler tarariSchemaHandler,
                       final SchemaSourceResolver[] schemaSourceResolvers,
                       final EntityResolver schemaEntityResolver ) {
        if ( config == null ) throw new NullPointerException();
        this.config = config;
        this.tarariSchemaHandler = tarariSchemaHandler;

        final List<SchemaSourceResolver> schemaSourceResolverList = new ArrayList<SchemaSourceResolver>();
        schemaSourceResolverList.add( new RegisteredSchemaSourceResolver( registeredSchemasByUri, registeredSchemasByTargetNamespace ) );
        schemaSourceResolverList.addAll( Arrays.asList( schemaSourceResolvers ) );
        this.schemaSourceResolvers = Collections.unmodifiableCollection( schemaSourceResolverList );
        this.schemaEntityResolver = schemaEntityResolver;

        if (timer == null)
            timer = new Timer("Schema cache maintenance", true);
        maintenanceTimer = timer;

        updateCacheConfiguration();

        for ( final SchemaSourceResolver schemaSourceResolver : this.schemaSourceResolvers ) {
            schemaSourceResolver.registerInvalidationListener( this );
        }
    }

    private synchronized void updateCacheConfiguration() {
        if (this.cacheCleanupTask != null) {
            this.cacheCleanupTask.cancel();
            this.cacheCleanupTask = null;
        }

        if (this.hardwareReloadTask != null) {
            this.hardwareReloadTask.cancel();
            this.hardwareReloadTask = null;
        }

        cacheCleanupTask = new SafeTimerTask() {
            @Override
            public void doRun() {
                cacheCleanup();
            }
        };
        maintenanceTimer.schedule(cacheCleanupTask, 4539, Math.min( minCleanupPeriod, config.getMaxCacheAge() * 2 + 263 ) );

        if ( config.isAllowDoctype() ) {
            entityResolverRef.set( schemaEntityResolver );
            schemaSourceTransformerRef.set( new SchemaSourceTransformer() );
        } else {
            entityResolverRef.set( XmlUtil.getSafeEntityResolver() );
            schemaSourceTransformerRef.set( new SafeSchemaSourceTransformer() );
        }

        if (tarariSchemaHandler != null) {
            hardwareReloadTask = new SafeTimerTask() {
                @Override
                public void doRun() {
                    maybeRebuildHardwareCache(0);
                }
            };
            maintenanceTimer.schedule(hardwareReloadTask, 1000, config.getHardwareRecompileLatency());
        }
    }

    @Override
    public void onApplicationEvent( final ApplicationEvent event ) {
        if ( event instanceof SchemaConfiguration.SchemaConfigurationReloadedEvent ) {
            logger.info("Rebuilding schema cache due to change in cache configuration");
            try {
                updateCacheConfiguration();
            } catch (Exception e) {
                logger.log( Level.SEVERE, "Error while rebuilding schema cache: " + ExceptionUtils.getMessage(e), e);
            }
        }
    }

    /**
     * Invalidate a schema. If any previous schema with this URI was known, it and any
     * schemas that make use of it will need to be recompiled before their next use.
     *
     * @param uri  the URI for the schema to invalidate.  Must not be null.
     */
    private void invalidateSchemaForUri( final String uri ) {
        final SchemaHandle old = schemasBySystemId.remove(uri);
        if (old != null) {
            schemasRecentlySuperseded.put(uri, 1);
            deferredCloseHandle(old);
        }
    }

    /**
     * Is the schema for the given URI past the given stale expiry?
     *
     * @param uri The URI for the schema to check.
     * @param time The time to check for expiry.
     * @return true if expired or the URL is not known.
     */
    private boolean isStaleExpired( final String uri, final long time ) {
        final SchemaHandle old = schemasBySystemId.get(uri);

        return old == null || (config.getMaxStaleAge() > -1 &&
               (time - old.getCompiledSchema().getCreatedTime()) > config.getMaxStaleAge());
    }

    private synchronized void deferredCloseHandle( final SchemaHandle handle ) {
        handlesNeedingClosed.put(handle, null);
    }

    private void cacheCleanup() {
        logger.finer("Running periodic schema cache cleanup task");

        closeDeferred();

        // Do some cache maintenance.  We'll decide what to do while holding the read lock;
        // then, we'll grab the write lock and do it.
        final Map<String, SchemaHandle> urisToRemove = new HashMap<String, SchemaHandle>();

        cacheLock.readLock().lock();
        try {
            final long maxCacheEntries = config.getMaxCacheEntries() + registeredSchemasByUri.size();

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

                            final Long leftTime = leftCs.getLastUsedTime();
                            final Long rightTime = rightCs.getLastUsedTime();
                            return leftTime.compareTo(rightTime);
                        }
                    });

                    for ( final SchemaHandle handle : handles ) {
                        if ( urisToRemove.size() >= extras ) break;
                        final CompiledSchema schema = handle.getTarget();
                        if ( schema != null && schema.isTransientlyReferencedSchema() ) {
                            urisToRemove.put(schema.getSystemId(), handle);
                        }
                    }
                }
            }

            // Then scan for any duplicate-TNS-causers that haven't been used in a while
            final long now = System.currentTimeMillis();
            final long maxAge = config.getMaxCacheAge() * 4;
            for (final SchemaHandle schemaHandle : schemasBySystemId.values()) {
                final CompiledSchema schema = schemaHandle.getTarget();
                if (schema != null && schema.isTransientSchema()) {
                    final long lastUsed = schema.getLastUsedTime();
                    final long useAge = now - lastUsed;
                    if (useAge > maxAge) {
                        // It hasn't been used in a while.  Is it contributing to a TNS conflict?
                        if ( schema.isConflictingTns() ) {
                            // It is Part Of The Problem.  Throw it out until someone wants it again.
                            urisToRemove.put(schema.getSystemId(), schemaHandle);
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
            for (final Map.Entry<String, SchemaHandle> entry : urisToRemove.entrySet()) {
                final String uri = entry.getKey();
                final SchemaHandle handle = entry.getValue();

                // Make sure it didn't get replaced while we were waiting for the write lock
                SchemaHandle old = schemasBySystemId.get(uri);
                if (old == handle) {
                    if (logger.isLoggable(Level.FINE)) logger.fine("Invalidating cached schema with systemId " + uri);
                    old = schemasBySystemId.remove(uri);
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
                final String uri = schema.getSystemId();
                try {
                    for ( final SchemaSourceResolver source : schemaSourceResolvers ) {
                        if ( source.getId().equals( schema.getSchemaSourceResolverId() ) ) {
                            source.refreshSchemaByUri( new LogOnlyAuditor(logger), uri );
                        }
                    }
                } catch ( IOException e ) {
                    invalidateSchemaForUri( uri );
                    logger.log( Level.WARNING,
                            "Error accessing schema for uri '"+ uri +"': " + ExceptionUtils.getMessage(e),
                            ExceptionUtils.getDebugException(e));
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
    private void getParents( final String uri, final Set<CompiledSchema> collected ) {
        final Collection<SchemaHandle> allHandles = new ArrayList<SchemaHandle>(schemasBySystemId.values());
        for (final SchemaHandle schemaHandle : allHandles) {
            final CompiledSchema schema = schemaHandle.getTarget();
            if (schema == null) continue;
            final Collection<SchemaHandle> dependencies = schema.getDependencies().values();
            for (final SchemaHandle handle : dependencies) {
                final CompiledSchema depSchema = handle.getTarget();
                if (depSchema == null) continue;
                if (uri.equals(depSchema.getSystemId())) {
                    visitAllParentsRecursive(depSchema, collected);
                }
            }
        }
    }

    /**
     * @return schema for URI, creating it if necessary.
     *         This is a new handle duped just for the caller; caller must close it when they are finished with it.
     */
    @Override
    public SchemaHandle getSchemaByUri( final Audit audit,
                                        final String uri ) throws IOException, SAXException {
        cacheLock.readLock().lock();
        try {
            final SchemaHandle ret = getSchemaByUriNoCompile(uri);
            if (ret != null) return ret;
        } finally {
            cacheLock.readLock().unlock();
        }

        // Cache miss.  We'll need to compile a new instance of this schema.
        final SchemaSource schemaSource = getSchemaSourceForUri(audit, uri, uri, true, true, false, true);
        assert schemaSource.getContent() != null;

        // We'll prevent other threads from compiling new schemas concurrently, to avoid complications with other
        // approaches:
        // No lock: Thundering herd of threads all compiling the same new schema at the same time
        // One-lock-per-schema-URI: results in deadlock:
        //     - Thread A compiling Schema A needs to import (and compile) Schema B; while at the same time,
        //     - Thread B compiling Schema B needs to import (and compile) Schema A
        cacheLock.writeLock().lock();
        try {
            // See if anyone else got it while we were waiting for the compiler mutex
            final SchemaHandle ret = getSchemaByUriNoCompile(uri);
            if (ret != null) return ret;

            return compileAndCache(audit, uri, schemaSource);
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    /**
     * Check for a cached schema.
     * <p/>
     * Caller must hold at least the read lock.
     *
     * @param uri the URI to look up in the cache.  Must not be null.
     * @return the already-compiled cached schema for this uri, or null if there isn't one.
     *         If a handle is returned, it will be a new handle duped just for the caller.
     *         Caller must close the handle when they are finished with it.
     */
    private SchemaHandle getSchemaByUriNoCompile( final String uri ) {
        final SchemaHandle handle = schemasBySystemId.get(uri);
        if (handle != null) {
            final CompiledSchema schema = handle.getTarget();
            if (schema != null) return schema.ref();
        }
        return null;
    }

    /**
     * Get the schema text for the specified URI.  This may require a network fetch if the schema is remote.
     *
     * @param audit         the audit to use
     * @param baseUri       the base URI, for resolving uri if it is relative.  Must not be null -- if there's no base URI,
     *                      pass uri as the base uri.
     * @param uri           the URI to resolve.  Must not be null.  May be relative to baseUri.  If it's not relative,
     *                      or if it is relative but matches as-is, baseUri will be ignored.
     * @param policyOk      if true, uris of the form "policy:whatever" will be allowed.  This should be allowed
     *                      only if this is a top-level fetch; otherwise, policies can import each others static schemas,
     *                      possibly in violation of access control partitions.
     * @param remoteOk      true to allow any remote URLs, false to verify remote URLs
     * @param remoteForbidden true if remote access is forbidden for the source.
     * @param requireSource true if a schema source is required, false to allow a null return.
     * @return as LSInput that contains both StringData and a SystemId.  Never null.
     * @throws IOException  if schema source could not be found for the specified URI.
     */
    private SchemaSource getSchemaSourceForUri( final Audit audit,
                                                final String baseUri,
                                                String uri,
                                                final boolean policyOk,
                                                final boolean remoteOk,
                                                final boolean remoteForbidden,
                                                final boolean requireSource ) throws IOException {
        if (!policyOk && isPolicySchemaUri( uri ) )
            throw new IOException("Schema URI not permitted in this context: " + uri );

        // Find a local schema by exact uri
        for ( final SchemaSourceResolver source : schemaSourceResolvers ) {
            if ( !source.isRemote() ) {
                final SchemaSource schemaSource = source.getSchemaByUri( audit, uri );
                if ( schemaSource != null ) return schemaSource;
            }
        }

        // Find a local schema by resolved uri
        uri = computeEffectiveUrl( baseUri, uri );
        for ( final SchemaSourceResolver source : schemaSourceResolvers ) {
            if ( !source.isRemote() ) {
                final SchemaSource schemaSource = source.getSchemaByUri( audit, uri );
                if ( schemaSource != null ) return schemaSource;
            }
        }

        // Not a local schema, try to resolve a remote schema
        if ( !remoteForbidden || allowRemoteReferencesFromLocal ) {
            if ( !remoteOk ) {
                validateRemoteSchemaUrl( uri );
            }
            for ( final SchemaSourceResolver source : schemaSourceResolvers ) {
                if ( source.isRemote() ) {
                    final SchemaSource schemaSource = source.getSchemaByUri( audit, uri );
                    if ( schemaSource != null ) return schemaSource;
                }
            }
        }

        if ( requireSource )
            throw new IOException("Unable to resolve schema " + describeResource( baseUri, uri ));

        return null;
    }

    /**
      * Get the schema text for the specified namespace.
      *
      * @param audit         the audit to use
      * @param baseUri       the base URI, for resolving uri if it is relative.  Must not be null -- if there's no base URI,
      *                      pass uri as the base uri.
      * @param namespaceUri  the namespace to resolve, which may be null.  
      * @param policyOk      if true, uris of the form "policy:whatever" will be allowed.  This should be allowed
      *                      only if this is a top-level fetch; otherwise, policies can import each others static schemas,
      *                      possibly in violation of access control partitions.
      * @param remoteOk      true to allow any remote URLs, false to verify remote URLs
      * @param remoteForbidden true if remote access is forbidden for the source.
      * @return as LSInput that contains both StringData and a SystemId.  Never null.
      * @throws IOException  if schema source could not be found for the specified URI.
      */
    private SchemaSource getSchemaSourceForNamespace( final Audit audit,
                                                      final String baseUri,
                                                      final String namespaceUri,
                                                      final boolean policyOk,
                                                      final boolean remoteOk,
                                                      final boolean remoteForbidden ) throws IOException {
        final String resolvedSystemId = findUriForTargetNamespace( audit, namespaceUri );
        if ( resolvedSystemId != null ) {
            return getSchemaSourceForUri( audit, baseUri, resolvedSystemId, policyOk, remoteOk, remoteForbidden, true );
        } else {
            throw new CausedIOException("Unable to resolve schema for namespace: " + namespaceUri);
        }
    }

    private boolean isPolicySchemaUri( final String uri ) {
        return uri.trim().toLowerCase().startsWith("policy:") || uri.contains( "policy:assertion:schemaval:sa" );
    }

    private void validateRemoteSchemaUrl( final String url ) throws IOException {
        final Pattern pattern = config.getRemoteResourcePattern();
        if ( pattern == null || !pattern.matcher(url).matches() ) {
            // Context information is added when this exception is handled
            throw new IOException("Remote resource access forbidden.");
        }
    }

    @SuppressWarnings({ "ThrowableInstanceNeverThrown" })
    private LSInput resolveEntity( final String publicId,
                                   final String systemId,
                                   final String baseURI ) {
        final EntityResolver resolver = entityResolverRef.get();

        final LSInput input = new LSInputImpl();
        try {
            InputSource inputSource;
            if ( resolver instanceof EntityResolver2 ) {
                inputSource = ((EntityResolver2)resolver).resolveEntity( "[dtd]", publicId, baseURI, systemId );   
            } else if ( baseURI != null ) {
                inputSource = resolver.resolveEntity( publicId, computeEffectiveUrl( baseURI, systemId ) );
            } else {
                inputSource = resolver.resolveEntity( publicId, systemId );
            }

            if ( inputSource != null ) {
                input.setPublicId( publicId );
                input.setSystemId( inputSource.getSystemId() );
                input.setEncoding( inputSource.getEncoding() );

                final Reader characterStream = inputSource.getCharacterStream();
                if ( characterStream != null ) {
                    input.setCharacterStream( characterStream );
                } else {
                    final InputStream byteStream = inputSource.getByteStream();
                    if ( byteStream != null ) {
                        input.setByteStream( byteStream );
                    } else {
                        throw new UnresolvableException( null, describeResource(baseURI, systemId, publicId, null));
                    }
                }
            } else {
                throw new UnresolvableException( null, describeResource(baseURI, systemId, publicId, null));
            }
        } catch ( SAXException e ) {
            handleResourceNotPermitted( e, describeResource(baseURI, systemId, publicId, null) );
            input.setCharacterStream( new IOExceptionThrowingReader( new CausedIOException(e) ) );
        } catch ( IOException e ) {
            handleResourceNotPermitted( e, describeResource(baseURI, systemId, publicId, null) );
            input.setCharacterStream( new IOExceptionThrowingReader( e ) );
        }

        return input;
    }

    protected void handleResourceNotPermitted( final Exception e,
                                               final String resourceDescription ) throws UnresolvableException {
        if ( isResourceNotPermitted( e ) ) {
            final Exception detail = config.isAllowDoctype() ? null : getUnresolvableDoctypeException();
            throw new UnresolvableException( detail, resourceDescription );
        }
    }

    private static Exception getUnresolvableDoctypeException() {
        return new Exception( "Use of document type definitions in XML Schemas is disabled (schema.allowDoctype cluster property)" );
    }

    protected boolean isResourceNotPermitted( final Exception e ) {
        return (e instanceof IOException || e instanceof SAXException) &&
                ExceptionUtils.getMessage( e ).startsWith("Document referred to an external entity with system id");
    }

    private InputSource makeInputSource( final SchemaResource schemaResource ) {
        final InputSource inputSource = new InputSource();
        inputSource.setSystemId( schemaResource.getUri() );
        inputSource.setCharacterStream( new StringReader( schemaResource.getContent() ) );
        return inputSource;
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
    public synchronized void registerSchema( final String uri,
                                             final String tns,
                                             final String schemaDoc ) {
        if (schemaDoc == null) throw new NullPointerException("A schema must be provided");
        final SchemaResource newResource = new SchemaResource(uri,tns,schemaDoc);
        final SchemaResource oldResource = registeredSchemasByUri.put(uri, newResource);
        if ( !newResource.equals(oldResource) ) {
            if ( tns != null ) {
                Set<SchemaResource> schemas = registeredSchemasByTargetNamespace.get( tns );
                if ( schemas == null ) {
                    schemas = new HashSet<SchemaResource>();
                    registeredSchemasByTargetNamespace.put( tns, schemas );
                }
                schemas.add( newResource );
            }
            if (oldResource != null)
                invalidateSchemaForUri(uri);
        }
    }

    @Override
    public synchronized void unregisterSchema( final String uri ) {
        final SchemaResource old = registeredSchemasByUri.remove(uri);
        if (old != null) {
            final String tns = old.getTargetNamespace();
            if ( tns != null ) {
                final Set<SchemaResource> schemas = registeredSchemasByTargetNamespace.get( tns );
                if ( schemas != null ) {
                    schemas.remove( old );
                }
            }
            invalidateSchemaForUri(uri);
        }
    }

    @Override
    public void registerUri( final String uri ) {
        synchronized( registeredUris ) {
            Integer count = registeredUris.get( uri );
            if ( count == null ) {
                count = 0;
            }
            registeredUris.put( uri, count + 1 );
        }
    }

    @Override
    public void unregisterUri( final String uri ) {
        synchronized( registeredUris ) {
            final Integer count = registeredUris.get( uri );
            if ( count != null && count > 1 ) {
                registeredUris.put( uri, count - 1 );
            } else {
                registeredUris.remove( uri );   
            }
        }
    }

    @Override
    public boolean isSchemaRegistered( final String uri ) {
        boolean active = false;

        final Set<String> registeredUris;
        synchronized( this.registeredUris ) {
            registeredUris = new HashSet<String>( this.registeredUris.keySet() );
        }

        if ( registeredUris.contains( uri ) || registeredSchemasByUri.containsKey( uri )) {
            active = true;
        } else {
            final SchemaHandle handle = schemasBySystemId.get( uri );
            if ( handle != null ) {
                final CompiledSchema schema = handle.getTarget();
                if ( schema != null ) {
                    if ( !schema.isTransientlyReferencedSchema()  ) {
                        active = true;
                    } else {
                        for ( final CompiledSchema export : schema.getExports() ) {
                            if ( registeredUris.contains( export.getSystemId() ) ) {
                                active = true;
                                break;                                
                            }
                        }
                    }
                }
            }
        }

        return active;
    }

    @Override
    public Set<String> getRequiredSchemaUris() {
        final Set<String> requiredUris;
        synchronized( this.registeredUris ) {
            requiredUris = new HashSet<String>( this.registeredUris.keySet() );
        }

        // for registered schemas both the schema and it direct dependencies are required
        // this covers the use case of hardware schemas (from global resources) and those
        // registered by schema validation policy assertions (when only the dependencies
        // are global schemas)
        for ( final Map.Entry<String,SchemaResource> uriAndResource : registeredSchemasByUri.entrySet() ) {
            final String uri = uriAndResource.getKey();
            if ( !isPolicySchemaUri( uri ) ) {
                requiredUris.add( uri );
            }

            boolean dependenciesHandled = false;
            final SchemaHandle handle = schemasBySystemId.get( uriAndResource.getKey() );
            if ( handle != null ) {
                final CompiledSchema schema = handle.getTarget();
                if ( schema != null ) {
                    dependenciesHandled = true;
                    requiredUris.addAll( schema.getDependencies().keySet() );
                }
            }

            if ( !dependenciesHandled ) { // the registered schema is not compiled, so find the dependencies now
                final SchemaResource schemaResource = uriAndResource.getValue();
                try {
                    // parse and permit registered DTDs, we still want the dependency
                    // information even if the schema is invalid due to DTDs being disabled
                    final Collection<ResourceReference> references =
                            SchemaUtil.getDependencies(makeInputSource(schemaResource), schemaEntityResolver);
                    for ( final ResourceReference reference : references ) {
                        if ( reference.getUri() !=null ) {
                            try {
                                final URI base = new URI( reference.getBaseUri() );
                                requiredUris.add( base.resolve( new URI(reference.getUri())).toString() );
                            } catch ( URISyntaxException e ) {
                                logger.log( Level.INFO,
                                        "Unable to resolve '"+reference.getUri()+"', against '"+reference.getBaseUri()+"', error is: " + ExceptionUtils.getMessage( e ),
                                        ExceptionUtils.getDebugException(e) );
                                requiredUris.add( reference.getUri() );
                            }
                        }
                    }
                } catch ( SAXException e ) {
                    logger.log( Level.INFO,
                            "Unable to parse XML Schema '"+schemaResource.getUri()+"', dependency info not available, error is: " + ExceptionUtils.getMessage( e ),
                            ExceptionUtils.getDebugException(e) );
                } catch ( IOException e ) {
                    logger.log( Level.INFO,
                            "Unable to parse XML Schema '"+schemaResource.getUri()+"', dependency info not available, error is: " + ExceptionUtils.getMessage( e ),
                            ExceptionUtils.getDebugException(e) );
                }
            }
        }

        return requiredUris;
    }

    @Override
    public Set<String> getRequiredSchemaTargetNamespaces() {
       final Set<String> requiredTargetNamespaces = new HashSet<String>();

        for ( final Map.Entry<String,SchemaResource> uriAndResource : registeredSchemasByUri.entrySet() ) {
            final boolean isPolicySchema = isPolicySchemaUri( uriAndResource.getKey() );

            boolean dependenciesHandled = false;
            final SchemaHandle handle = schemasBySystemId.get( uriAndResource.getKey() );
            if ( handle != null ) {
                final CompiledSchema schema = handle.getTarget();
                if ( schema != null ) {
                    dependenciesHandled = true;
                    if (!isPolicySchema) requiredTargetNamespaces.add( schema.getTargetNamespace() );
                    for ( final String uri : schema.getDependencies().keySet() ) {
                        final SchemaHandle dependencyHandle = schemasBySystemId.get( uri );
                        if ( dependencyHandle != null ) {
                            final CompiledSchema dependencySchema = dependencyHandle.getTarget();
                            if ( dependencySchema != null ) {
                                requiredTargetNamespaces.add( dependencySchema.getTargetNamespace() );
                            }
                        }
                    }
                }
            }

            if ( !dependenciesHandled ) { // the registered schema is not compiled, so find the dependencies now
                final SchemaResource schemaResource = uriAndResource.getValue();
                try {
                    if (!isPolicySchema) requiredTargetNamespaces.add( XmlUtil.getSchemaTNS( schemaResource.getContent() ) );

                    // parse and permit registered DTDs, we still want the dependency
                    // information even if the schema is invalid due to DTDs being disabled
                    final Collection<ResourceReference> references =
                            SchemaUtil.getDependencies(makeInputSource(schemaResource), schemaEntityResolver);
                    for ( final ResourceReference reference : references ) {
                        if ( reference.hasTargetNamespace() ) {
                            requiredTargetNamespaces.add( reference.getTargetNamespace() );
                        }
                    }
                } catch ( SAXException e ) {
                    logger.log( Level.INFO,
                            "Unable to parse XML Schema '"+schemaResource.getUri()+"', dependency info not available, error is: " + ExceptionUtils.getMessage( e ),
                            ExceptionUtils.getDebugException(e) );
                } catch ( IOException e ) {
                    logger.log( Level.INFO,
                            "Unable to parse XML Schema '"+schemaResource.getUri()+"', dependency info not available, error is: " + ExceptionUtils.getMessage( e ),
                            ExceptionUtils.getDebugException(e) );
                } catch ( XmlUtil.BadSchemaException e ) {
                    logger.log( Level.INFO,
                            "Unable to parse XML Schema '"+schemaResource.getUri()+"', dependency info not available, error is: " + ExceptionUtils.getMessage( e ),
                            ExceptionUtils.getDebugException(e) );
                }
            }
        }

        return requiredTargetNamespaces;
    }

    @Override
    public boolean invalidateSchemaByUri( final String uri, final boolean validReplacement ) {
        boolean invalidated = false;
        if ( validReplacement || isStaleExpired( uri, System.currentTimeMillis() ) ) {
            invalidateSchemaForUri( uri );
            invalidated = true;
        }
        return invalidated;
    }

    @ManagedAttribute(description="Schema Cache Size", currencyTimeLimit=5)
    public int getSchemaCacheSize() {
        return schemasBySystemId.size();
    }

    @ManagedOperation(description="Schema Cache Keys")
    public Set<String> getSchemaCacheUris() {
        return new TreeSet<String>(schemasBySystemId.keySet());
    }

    @ManagedOperation(description="Rebuild Hardware Cache")
    public String rebuildHardwareCache() {
        String message;
        if ( tarariSchemaHandler == null ) {
            message = "Hardware not available.";
        } else if ( cacheLock.writeLock().tryLock() ) {
            message = "Cache rebuilt.";
            try {
                doRebuild();
            } catch( Exception e ) {
                logger.log( Level.WARNING, "Error rebuilding cache.", e );               
            } finally {
                cacheLock.writeLock().unlock();
            }
        } else {
            message = "Unable to lock cache, please try again.";
        }
        return message;
    }

    @ManagedAttribute(description="Registered Schema Size", currencyTimeLimit=5)
    public int getRegisteredSchemaSize() {
        return registeredSchemasByUri.size();
    }

    @ManagedOperation(description="Registered Schema Keys")
    public Set<String> getRegisteredSchemaUris() {
        return new TreeSet<String>(registeredSchemasByUri.keySet());
    }

    private static String describeResource( final String baseURI, final String uri ) {
        return describeResource(baseURI, uri, null, null);
    }

    private static String describeResource( final String baseURI,
                                            final String systemId,
                                            final String publicId,
                                            final String namespaceURI ) {
        final String description;

        if (systemId != null) {
            String resourceUri = systemId;
            if (baseURI != null) {
                try {
                    // build uri for use in description only
                    resourceUri = new URI(baseURI).resolve(systemId).toString();
                } catch (URISyntaxException e) {
                    if (logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE,
                                "Unable to resolve uri ''{0}'', relative to base uri ''{1}''.",
                                new String[]{systemId, baseURI});
                    }
                } catch (IllegalArgumentException e) {
                    if (logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE,
                                "Unable to resolve uri ''{0}'', relative to base uri ''{1}''.",
                                new String[]{systemId, baseURI});
                    }
                }
            }

            description = "URI:" + resourceUri;
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
     * @param source The source for the schema document
     * @return the dependencies directly or indirectly used by this schema.  Never null.
     * @throws SAXException if a schema is not valid
     * @throws IOException if a remote schema cannot be fetched
     */
    private Set<String> preCacheSchemaDependencies( final Audit audit,
                                                    final SchemaSource source ) throws SAXException, IOException {
        final Set<String> dependencies = new HashSet<String>();

        final Set<String> localDependencies = new HashSet<String>();
        if ( !isRemoteSource( source.getResolverId() ) ) {
            localDependencies.add( source.getUri() );
        }

        final SchemaFactory sf = SchemaFactory.newInstance(XmlUtil.W3C_XML_SCHEMA);
        final LSResourceResolver resolver = new LSResourceResolver() {
            @Override
            public LSInput resolveResource(final String type,
                                           final String namespaceURI,
                                           final String publicId,
                                           final String systemId,
                                           final String baseURI)
            {
                if ( XMLConstants.XML_DTD_NS_URI.equals( type )) {
                    return resolveEntity( publicId, systemId, baseURI );
                } else {
                    try {
                        SchemaSource dependencySource = null;
                        final boolean remoteForbidden = localDependencies.contains(baseURI);
                        if ( systemId != null ) {
                            dependencySource = getSchemaSourceForUri(audit, baseURI, systemId, false, false, remoteForbidden, false );
                        }
                        if ( dependencySource == null ) {
                            dependencySource = getSchemaSourceForNamespace( audit, baseURI, namespaceURI, false, false, remoteForbidden);
                        }

                        assert dependencySource != null;
                        if ( !isRemoteSource( dependencySource.getResolverId() ) ) {
                            localDependencies.add( source.getUri() );
                        }
                        dependencies.add(dependencySource.getUri());
                        return schemaSourceTransformerRef.get().makeLsInput(dependencySource);
                    } catch (IOException e) {
                        throw new UnresolvableException(e, describeResource(baseURI, systemId, publicId, namespaceURI));
                    }
                }
            }
        };
        sf.setResourceResolver(resolver);

        try {
            sf.newSchema(schemaSourceTransformerRef.get().makeSource(source)); // populates dependencies as side-effect
            return dependencies;
        } catch (RuntimeException e) {
            final UnresolvableException exception = ExceptionUtils.getCauseIfCausedBy(e, UnresolvableException.class);
            if (exception != null) throw new CausedIOException(
                    "Unable to resolve dependency for resource " + exception.getResourceDescription() +
                    " : " + ExceptionUtils.getMessage(exception.getCause()),
                    exception.getCause());
            throw e;
        }
    }

    private String findUriForTargetNamespace( final Audit audit,
                                              final String targetNamespace ) throws IOException {
        // Find a local schema by targetNamespace
        for ( final SchemaSourceResolver source : schemaSourceResolvers ) {
            if ( !source.isRemote() ) {
                final SchemaSource schemaSource = source.getSchemaByTargetNamespace( audit, targetNamespace );
                if ( schemaSource != null ) return schemaSource.getUri();
            }
        }

        return null;
    }

    private boolean isRemoteSource( final String resolverId ) throws IOException {
        boolean remote = false;

        for ( final SchemaSourceResolver source : schemaSourceResolvers ) {
            if ( source.getId().equals( resolverId )) {
                remote = source.isRemote();
                break;
            }
        }

        return remote;
    }

    /**
     * Compute effective URL when relative is evaluated in the context of base.
     *
     * @param base      base url, ie "http://foo.com/blah/blortch.xsd".  Must not be null.
     * @param relative  URL that may be relative to base (ie, "blo/bletch.xsd") or may be absolute.  Must not be null.
     * @return  the effective URL when relative is evaluated relative to base.  Never null.
     * @throws NullPointerException if either base or relative is null.
     * @throws IOException if either URI in invalid.
     */
    private String computeEffectiveUrl( final String base,
                                        final String relative ) throws IOException {
        if (base == null || relative == null) throw new NullPointerException();

        try {
            final URI relativeUri = new URI(relative);
            final URI baseUrl = new URI(base);
            return baseUrl.resolve( relativeUri ).toString();
        }
        catch(URISyntaxException e) {
            throw new IOException( e );
        }
    }

    /**
     * Compile this schema document.  This may require fetching remote schemas.
     * Caller must hold the write lock.
     *
     * @return a new handle, duped just for the caller.  Caller must close it when they are finished with it.
     * @throws SAXException
     */
    private SchemaHandle compileAndCache( final Audit audit,
                                          final String systemId,
                                          final SchemaSource source )
            throws SAXException, IOException
    {
        // Do initial parse and get dependencies (strings are all hot in the HTTP cache after this)
        preCacheSchemaDependencies(audit, source);

        invalidateParentsOfRecentlySupersededSchemas();

        SchemaHandle schemaHandle = compileAndCacheRecursive(audit, systemId, source, new HashSet<String>());
        maybeEnableHardwareForNewSchema(schemaHandle.getTarget());
        return schemaHandle;
    }

    /** Caller must hold the write lock. */
    private void invalidateParentsOfRecentlySupersededSchemas() {
        // Invalidate parents of anything reloaded since last time we checked something
        while (!schemasRecentlySuperseded.isEmpty()) {
            List<String> strings = new ArrayList<String>(schemasRecentlySuperseded.keySet());
            schemasRecentlySuperseded.clear();
            for (String uri : strings) {
                // This guy was reloaded or thrown out.  Make sure all parents are invalidated.
                Set<CompiledSchema> parents = new HashSet<CompiledSchema>();
                getParents(uri, parents);
                for (CompiledSchema schema : parents) {
                    if (!schema.isClosed()) invalidateSchemaForUri(schema.getSystemId());
                }
            }
        }
    }

    /**
     * Build a new CompiledSchema for the specified URI and enter it into the cache.  Caller guarantees
     * that any remote schemas are hot in the cache, and any schemas that are in need of a recompile due to
     * changed remote schemas have already been invalidated and removed from schemasBySystemId.
     * <p/>
     * This will recursively compile the schemas dependencies.
     * <p/>
     * Caller must hold the write lock.
     *
     * @param systemId    systemId of the schema being compiled. required
     * @param source      the source for the schema XML
     * @param seenSystemIds  the set of system IDs seen since the current top-level compile began
     * @return a SchemaHandle to a new CompiledSchema instance, already duplicated for the caller.  Caller must close
     *         this handle when they are finished with it.
     */
    private SchemaHandle compileAndCacheRecursive( final Audit audit,
                                                   final String systemId,
                                                   final SchemaSource source,
                                                   final Set<String> seenSystemIds ) throws SAXException, IOException {
        if (seenSystemIds.contains(systemId)) {
            logger.info("Circular import detected.");
            return null;
        }

        final String schemaDoc = source.getContent();
        seenSystemIds.add(systemId);

        // Re-parse, building up CompiledSchema instances as needed from the bottom up
        final SchemaFactory sf = SchemaFactory.newInstance(XmlUtil.W3C_XML_SCHEMA);

        final Document schema = XmlUtil.parse(schemaSourceTransformerRef.get().makeInputSource(source), entityResolverRef.get());
        final Set<String> includes = new HashSet<String>();
        final DocumentReferenceProcessor schemaReferenceProcessor = DocumentReferenceProcessor.schemaProcessor();
        schemaReferenceProcessor.processDocumentReferences( schema, new DocumentReferenceProcessor.ReferenceCustomizer(){
            @Override
            public String customize( final Document document, 
                                     final Node node,
                                     final String documentUri,
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
                if ( XMLConstants.XML_DTD_NS_URI.equals( type )) {
                    return resolveEntity( publicId, systemId, baseURI );
                } else if ( !XMLConstants.W3C_XML_SCHEMA_NS_URI.equals(type) ) {
                    throw new UnresolvableException(null, describeResource(baseURI, systemId, publicId, namespaceURI));
                } else {
                    Map<String,SchemaHandle> dependencyMap = includes.contains(systemId) ? directIncludes : directImports;
                    try {
                        return schemaSourceTransformerRef.get().makeLsInput(resolveSchema( audit, namespaceURI, systemId, baseURI, seenSystemIds, dependencyMap ));
                    } catch (IOException e) {
                        throw new UnresolvableException(e, describeResource(baseURI, systemId, publicId, namespaceURI));
                    } catch (SAXException e) {
                        throw new UnresolvableException(e, describeResource(baseURI, systemId, publicId, namespaceURI));
                    }
                }
            }
        };
        sf.setResourceResolver(resolver);

        boolean success = false;
        try {
            final Schema softwareSchema = sf.newSchema(schemaSourceTransformerRef.get().makeSource(source));
            final String tns = XmlUtil.getSchemaTNS(systemId, schemaDoc, entityResolverRef.get());
            final Element mangledElement = DomUtils.normalizeNamespaces(schema.getDocumentElement());
            final CompiledSchema newSchema =
                    new CompiledSchema(tns, systemId, schemaDoc, source.getResolverId(), mangledElement, softwareSchema, this,
                            directImports, directIncludes, source.isTransient(), config.isSoftwareFallback());
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
            if (exception != null) throw new CausedIOException(
                    "Unable to resolve dependency for resource " + exception.getResourceDescription() +
                    " : " + ExceptionUtils.getMessage(exception.getCause()),
                    exception.getCause());
            throw e;
        } finally {
            if ( !success ) {
                for (SchemaHandle impHandle : CollectionUtils.iterable(directImports.values(), directIncludes.values())) {
                    impHandle.close();
                }
            }
        }
    }

    private SchemaSource resolveSchema( final Audit audit,
                                        final String namespaceURI,
                                        final String systemId,
                                        final String baseURI,
                                        final Set<String> seenSystemIds,
                                        final Map<String, SchemaHandle> dependencyMap ) throws IOException, SAXException {
        SchemaSource source = null;
        if ( systemId != null ) {
            source = getSchemaSourceForUri(audit, baseURI, systemId, false, false, false, false);
        }
        if ( source == null ) {
            source = getSchemaSourceForNamespace( audit, baseURI, namespaceURI, false, false, false);
        }
        assert source != null;

        SchemaHandle handle = schemasBySystemId.get(source.getUri());

        if (handle == null) {
            // Have to compile a new one
            handle = compileAndCacheRecursive(audit, source.getUri(), source, seenSystemIds);
        } else {
            // Can't give it away while it remains in the cache -- need to dupe it (Bug #2926)
            handle = handle.getCompiledSchema().ref();
        }

        //if handle is null, there was a circular dependency.
        if ( handle != null ) {
            final CompiledSchema schema = handle.getCompiledSchema();
            dependencyMap.put(schema.getSystemId(), handle); // give it away without closing it
            return new DefaultSchemaSource(schema.getSystemId(), schema.getSchemaDocument(), schema.getSchemaSourceResolverId(), schema.isTransientSchema());
        }
        
        return source;
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
                // We were the active schema for this URI -- remove ourselves
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
        scheduleOneShotRebuildCheck( config.getHardwareRecompileMinAge());
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

        int hardwareRecompileLatency = config.getHardwareRecompileLatency();
        int hardwareRecompileMinAge = config.getHardwareRecompileMinAge();
        int hardwareRecompileMaxAge = config.getHardwareRecompileMaxAge();

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

    private static final class SchemaResource extends Triple<String,String,String> {
        private SchemaResource( final String uri, final String tns, final String content ) {
            super( uri, tns, content );
        }

        public String getUri() {
            return left;
        }

        public String getTargetNamespace() {
            return middle;
        }

        public String getContent() {
            return right;
        }
    }

    private static class SchemaSourceTransformer {
        /** @return an LSInput that contains StringData and a SystemId. */
        protected LSInput makeLsInput( final SchemaSource schemaSource ) {
            processingSource( schemaSource );

            final LSInput lsi =  new LSInputImpl();
            lsi.setSystemId( schemaSource.getUri() );
            lsi.setStringData( schemaSource.getContent() );
            return lsi;
        }

        protected Source makeSource( final SchemaSource schemaSource ) {
            processingSource( schemaSource );
            return new StreamSource( new StringReader(schemaSource.getContent()), schemaSource.getUri() );
        }

        protected InputSource makeInputSource( final SchemaSource schemaSource ) {
            processingSource( schemaSource );

            final InputSource inputSource = new InputSource();
            inputSource.setSystemId( schemaSource.getUri() );
            inputSource.setCharacterStream( new StringReader( schemaSource.getContent() ) );
            return inputSource;
        }

        protected void processingSource( final SchemaSource schemaSource ) {
        }
    }

    private static final class SafeSchemaSourceTransformer extends SchemaSourceTransformer {
        @Override
        protected void processingSource( final SchemaSource schemaSource ) {
            if ( strictDoctypeChecking && XmlUtil.hasDoctype( schemaSource.getContent() ) ) {
                throw new UnresolvableException( getUnresolvableDoctypeException(), describeResource( null, schemaSource.getUri() ) );
            }
        }
    }

    private static final class RegisteredSchemaSourceResolver implements SchemaSourceResolver {
        private final Map<String,SchemaResource> registeredSchemasByUri;
        private final Map<String,Set<SchemaResource>> registeredSchemasByTargetNamespace;

        private RegisteredSchemaSourceResolver( final Map<String,SchemaResource> registeredSchemasByUri,
                                                final Map<String,Set<SchemaResource>> registeredSchemasByTargetNamespace ) {
            this.registeredSchemasByUri = registeredSchemasByUri;
            this.registeredSchemasByTargetNamespace = registeredSchemasByTargetNamespace;
        }

        @Override
        public String getId() {
            return "Registered Resolver";
        }

        @Override
        public boolean isTransient() {
            return false;
        }

        @Override
        public boolean isRemote() {
            return false;
        }

        @Override
        public SchemaSource getSchemaByTargetNamespace( final Audit audit,
                                                        final String targetNamespace ) throws IOException {
            final SchemaSource schemaSource;
            final Set<SchemaResource> schemas = registeredSchemasByTargetNamespace.get( targetNamespace==null ? "" : targetNamespace );
            if ( schemas != null && !schemas.isEmpty() ) {
                if ( schemas.size() == 1 ) {
                    schemaSource = asSource(schemas.iterator().next());
                } else {
                    final Set<String> uris = Functions.reduce( schemas, new TreeSet<String>(), new Functions.Binary<Set<String>,Set<String>,SchemaResource>(){
                        @Override
                        public Set<String> call( final Set<String> uris, final SchemaResource schemaResource ) {
                            uris.add( schemaResource.getUri() );
                            return uris;
                        }
                    } );
                    throw new IOException( "Multiple schemas found for target namespace, system identifiers are " + uris );
                }
            } else {
                schemaSource = null;
            }
            return schemaSource;
        }

        @Override
        public SchemaSource getSchemaByUri( final Audit audit,
                                            final String uri ) {
            final SchemaResource schema = registeredSchemasByUri.get( uri );
            return schema != null ? asSource(schema) : null;
        }

        @Override
        public void refreshSchemaByUri( final Audit audit,
                                        final String uri ) {
        }

        @Override
        public void registerInvalidationListener( final SchemaInvalidationListener listener ) {
        }

        private SchemaSource asSource( final SchemaResource resource ) {
            return new DefaultSchemaSource(
                    resource.getUri(),
                    resource.getContent(),
                    this );
        }
    }
}

