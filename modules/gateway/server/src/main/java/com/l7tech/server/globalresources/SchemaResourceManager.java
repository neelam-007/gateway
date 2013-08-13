package com.l7tech.server.globalresources;

import com.l7tech.common.io.ResourceReference;
import com.l7tech.common.io.SchemaUtil;
import com.l7tech.gateway.common.audit.LoggingAudit;
import com.l7tech.gateway.common.resources.ResourceEntry;
import com.l7tech.gateway.common.resources.ResourceEntryHeader;
import com.l7tech.gateway.common.resources.ResourceType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.communityschemas.SchemaHandle;
import com.l7tech.server.communityschemas.SchemaManager;
import com.l7tech.server.event.EntityClassEvent;
import com.l7tech.server.util.PostStartupApplicationListener;
import com.l7tech.util.Config;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.Triple;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEvent;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides the glue between the ResourceEntryManager and SchemaManager.
 */
public class SchemaResourceManager implements PostStartupApplicationListener, InitializingBean, PropertyChangeListener {

    //- PUBLIC

    public SchemaResourceManager( final Config config,
                                  final Timer timer,
                                  final ResourceEntryManager resourceEntryManager,
                                  final SchemaManager schemaManager ) {
        this.config = config;
        this.timer = timer;
        this.resourceEntryManager = resourceEntryManager;
        this.schemaManager = schemaManager;
    }

    public boolean isSchemaRequired( final String uri ) {
        boolean required = false;
        final Set<String> requiredSchemaUris = schemaManager.getRequiredSchemaUris();

        if ( requiredSchemaUris.contains( uri ) ) {
            required = true;
        } else { // check if the schema is a dependency of a required schema
            for ( final String requiredSchemaUri : requiredSchemaUris ) {
                if ( isDependency( uri, requiredSchemaUri, new HashSet<String>() ) ) {
                    required = true;
                    break;
                }
            }

            if ( !required ) { // check for a target namespace reference
                final DependencyReferenceInfo info = resourceDependencies.get( uri );
                if ( info != null ) {
                    final Set<String> requiredSchemaTargetNamespaces = schemaManager.getRequiredSchemaTargetNamespaces();
                    if ( requiredSchemaTargetNamespaces.contains( info.targetNamespace ) ) {
                        required = true;
                    } else { // check if the schema is a dependency of a schema required by tns
                        outer:
                        for ( final String requiredTargetNamespace : requiredSchemaTargetNamespaces ) {
                            final Collection<DependencyReferenceInfo> dependencyReferences =
                                    resourceDependenciesByTns.get( tnsKey(requiredTargetNamespace) );
                            if ( dependencyReferences != null ) {
                                for ( final DependencyReferenceInfo dependencyReference : dependencyReferences ) {
                                    if ( isDependency( uri, dependencyReference.uri, new HashSet<String>() ) ) {
                                        required = true;
                                        break outer;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return required;
    }

    @Override
    public void propertyChange( final PropertyChangeEvent evt ) {
        invalidationTime.set( System.currentTimeMillis() );
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        synchronized ( reloadLock ) {
            reload();
        }

        timer.schedule( new TimerTask(){
            @Override
            public void run() {
                reloadIfRequired();
            }
        }, 15731, 5331 );
    }

    @Override
    public void onApplicationEvent( final ApplicationEvent event ) {
        if ( event instanceof EntityClassEvent) {
            final EntityClassEvent classEvent = (EntityClassEvent) event;
            if ( ResourceEntry.class.isAssignableFrom(classEvent.getEntityClass()) ) {
                invalidationTime.set( System.currentTimeMillis() );
            }
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( SchemaResourceManager.class.getName() );

    private final Config config;
    private final Timer timer;
    private final ResourceEntryManager resourceEntryManager;
    private final SchemaManager schemaManager;

    private final Map<String,DependencyReferenceInfo> resourceDependencies = new ConcurrentHashMap<String,DependencyReferenceInfo>();
    private final Map<TnsKey,Collection<DependencyReferenceInfo>> resourceDependenciesByTns = new ConcurrentHashMap<TnsKey,Collection<DependencyReferenceInfo>>();
    private final AtomicLong invalidationTime = new AtomicLong();
    private final Object reloadLock = new Object();
    private Set<String> hardwareSchemas = Collections.emptySet();
    private long lastReloadTime;

    /**
     *
     */
    private void reloadIfRequired() {
        final long invalidationTime = this.invalidationTime.get();
        final long lastReloadTime;
        synchronized( reloadLock ) {
            lastReloadTime = this.lastReloadTime;
        }

        if ( lastReloadTime < invalidationTime ) {
            synchronized ( reloadLock ) {
                reload();
            }
        }
    }

    private void reload() {
        updateReferenceCache();
        loadHardwareSchemas();
    }

    /**
     *
     */
    private void updateReferenceCache() {
        try {
            final Collection<ResourceEntry> resourceEntries = resourceEntryManager.findAll();

            final Set<String> processedUris = new HashSet<String>();
            final Set<String> processedTargetNamespaces = new HashSet<String>();
            for ( final ResourceEntry entry : resourceEntries ) {
                if ( entry.getType() != ResourceType.XML_SCHEMA ) continue;

                processedUris.add( entry.getUri() );
                processedTargetNamespaces.add( entry.getResourceKey1() );

                DependencyReferenceInfo info = resourceDependencies.get( entry.getUri() );
                if ( info == null || !Goid.equals(info.goid, entry.getGoid()) || info.version!=entry.getVersion() ) {
                    Collection<ResourceReference> references = Collections.emptyList();

                    try {
                        references = SchemaUtil.getDependencies( asInputSource(entry), new ResourceEntrySchemaSourceResolver(resourceEntryManager) );
                    } catch ( SAXException e ) {
                        logger.log( Level.WARNING,
                                "Error loading schema '"+entry.getUri()+"' to update dependency cache: " + ExceptionUtils.getMessage( e ),
                                ExceptionUtils.getDebugException( e ) );
                    } catch ( IOException e ) {
                        logger.log( Level.WARNING,
                                "Error loading schema '"+entry.getUri()+"' to update dependency cache: " + ExceptionUtils.getMessage( e ),
                                ExceptionUtils.getDebugException( e ) );
                    }

                    info = new DependencyReferenceInfo( entry.getGoid(), entry.getVersion(), entry.getUri(), entry.getResourceKey1(), references );

                    resourceDependencies.put( entry.getUri(), info );

                    final Collection<DependencyReferenceInfo> referencesForTns = resourceDependenciesByTns.get( tnsKey(entry.getResourceKey1()) );
                    if ( referencesForTns == null ) {
                        resourceDependenciesByTns.put( tnsKey(entry.getResourceKey1()), Collections.singleton( info ) );
                    } else {
                        final Collection<DependencyReferenceInfo> newReferencesForTns = new ArrayList<DependencyReferenceInfo>( referencesForTns );
                        newReferencesForTns.add( info );
                        resourceDependenciesByTns.put( tnsKey(entry.getResourceKey1()), Collections.unmodifiableCollection( newReferencesForTns ) );
                    }
                }
            }

            resourceDependencies.keySet().retainAll( processedUris );
            resourceDependenciesByTns.keySet().retainAll( processedTargetNamespaces );
        } catch ( FindException e ) {
            logger.log( Level.WARNING, "Error loading schemas to update dependency cache.", e );
        }
    }

    private InputSource asInputSource( final ResourceEntry schemaResource ) {
        final InputSource inputSource = new InputSource();
        inputSource.setSystemId( schemaResource.getUri() );
        inputSource.setCharacterStream( new StringReader( schemaResource.getContent() ) );
        return inputSource;
    }

    /**
     *
     */
    private void loadHardwareSchemas() {
        lastReloadTime = System.currentTimeMillis();
        final Set<String> schemasToRemove = new HashSet<String>();
        final Set<SchemaResource> schemasWithContent = new HashSet<SchemaResource>();

        boolean loadSuccessful = false;
        try {
            final Set<String> oldSchemaUrls = hardwareSchemas;
            final Set<String> schemaUrls = new HashSet<String>();
            final String[] targetNamespaces =
                    config.getProperty( "schema.hardwareTargetNamespaces", "" ).split( "(?m)\\s{1,}" );

            // Lookup Schema URLs for targetNamespaces
            final Map<String,ResourceEntryHeader> headersByUri = new HashMap<String,ResourceEntryHeader>();
            for ( final String targetNamespace : targetNamespaces ) {
                final Collection<ResourceEntryHeader> headers = resourceEntryManager.findHeadersByTNS( targetNamespace );

                if ( headers.size() == 1 ) {
                    final ResourceEntryHeader header = headers.iterator().next();
                    headersByUri.put( header.getUri(), header );
                    schemaUrls.add( header.getUri() );
                } else if ( headers.isEmpty() ) {
                    logger.warning( "XML Schema not found with target namespace '"+targetNamespace+"', unable to register for hardware." );
                } else {
                    logger.warning( "Multiple ("+headers.size()+") XML Schemas found with target namespace '"+targetNamespace+"', unable to register for hardware." );
                }
            }

            schemasToRemove.addAll( oldSchemaUrls );
            schemasToRemove.removeAll( schemaUrls );

            for ( final String uri : schemaUrls ) {
                final ResourceEntry resourceEntry = resourceEntryManager.findByHeader( headersByUri.get(uri) );
                if ( resourceEntry != null && resourceEntry.getType() == ResourceType.XML_SCHEMA ) {
                     schemasWithContent.add( new SchemaResource(resourceEntry.getUri(), resourceEntry.getResourceKey1(), resourceEntry.getContent()) );
                }
            }

            loadSuccessful = true;
        } catch ( FindException e ) {
            logger.log( Level.WARNING, "Error registering schemas for hardware.", e );
        }

        if ( loadSuccessful && (!schemasToRemove.isEmpty() || !schemasWithContent.isEmpty()) ) {
            // Update schema manager and track active schemas
            logger.info( "(Re)registering XML Schemas for hardware." );

            final Set<String> currentSchemaUrls = new HashSet<String>(hardwareSchemas);
            for ( final String uri : schemasToRemove ) {
                currentSchemaUrls.remove( uri );
                schemaManager.unregisterSchema( uri );
            }
            for ( final SchemaResource schema :  schemasWithContent ) {
                currentSchemaUrls.add( schema.getUri() );
                schemaManager.registerSchema( schema.getUri(), schema.getTargetNamespaceForRegistration(), schema.getContent() );
            }
            hardwareSchemas = Collections.unmodifiableSet( currentSchemaUrls );

            // Validate all hardware schemas
            for ( final String uri : hardwareSchemas ) {
                SchemaHandle handle = null;
                try {
                    handle = schemaManager.getSchemaByUri( new LoggingAudit(logger), uri );
                } catch ( SAXException e ) {
                    logger.log( Level.WARNING,
                            "Invalid XML Schema '"+uri+"' (when registering for hardware): " + ExceptionUtils.getMessage(e),
                            ExceptionUtils.getDebugException(e));
                } catch ( IOException e ) {
                    logger.log( Level.WARNING,
                            "Invalid XML Schema '"+uri+"' (when registering for hardware): " + ExceptionUtils.getMessage(e),
                            ExceptionUtils.getDebugException(e));
                } finally {
                    ResourceUtils.closeQuietly( handle );
                }
            }
        }
    }

    private boolean isDependency( final String dependencyUri,
                                  final String schemaUri,
                                  final Set<String> processedUris ) {
        boolean dependency = false;

        if ( !processedUris.contains( schemaUri )) {
            processedUris.add( schemaUri );
            final DependencyReferenceInfo dependencyInfo = resourceDependencies.get( dependencyUri );
            final DependencyReferenceInfo info = resourceDependencies.get( schemaUri );
            if ( info != null ) {
                final Set<String> resolvedUris = new HashSet<String>();
                for ( final ResourceReference reference : info.references ) {
                    if ( reference.getUri() != null && reference.getUri().equals(dependencyUri) ) {
                        dependency = true;
                        break;
                    }

                    if ( reference.getUri() != null && reference.getBaseUri() != null ) { // check resolved URI
                        try {
                            final String resolvedUri = new URI( reference.getBaseUri() ).resolve( reference.getUri() ).toString();
                            resolvedUris.add( resolvedUri );
                            if ( resolvedUri.equals(dependencyUri) ) {
                                dependency = true;
                                break;
                            }
                        } catch ( URISyntaxException e ) {
                            logger.log( Level.FINE,
                                    "Error resolving dependency URI '"+reference.getUri()+"' against '"+reference.getBaseUri()+"': " + ExceptionUtils.getMessage( e ),
                                    ExceptionUtils.getDebugException(e) );
                        } catch ( IllegalArgumentException e ) {
                            logger.log( Level.FINE,
                                    "Error resolving dependency URI '"+reference.getUri()+"' against '"+reference.getBaseUri()+"': " + ExceptionUtils.getMessage( e ),
                                    ExceptionUtils.getDebugException(e) );
                        }
                    }

                    if ( dependencyInfo != null && reference.hasTargetNamespace() &&
                            ((reference.getTargetNamespace()==null && dependencyInfo.targetNamespace==null) ||
                             (reference.getTargetNamespace()!=null && reference.getTargetNamespace().equals(dependencyInfo.targetNamespace))) ) {
                        dependency = true;
                        break;
                    }
                }

                if ( !dependency ) { // then check recursively
                    for ( final String uri : resolvedUris ) {
                        if ( isDependency( dependencyUri, uri, processedUris ) ) {
                            dependency = true;
                            break;
                        }
                    }
                }
            }
        }

        return dependency;
    }

    private static TnsKey tnsKey( final String targetNamespace ) {
        return new TnsKey( targetNamespace );
    }

    private static final class SchemaResource extends Triple<String,String,String> {
        private SchemaResource( final String uri, final String tns, final String content ) {
            super( uri, tns, content );
        }

        public String getUri() {
            return left;
        }

        public String getTargetNamespaceForRegistration() {
            return middle==null ? "" : middle;
        }

        public String getContent() {
            return right;
        }
    }

    private static final class DependencyReferenceInfo {
        private final Goid goid;
        private final int version;
        private final String uri;
        private final String targetNamespace;
        private final Collection<ResourceReference> references;

        private DependencyReferenceInfo( final Goid goid,
                                         final int version,
                                         final String uri,
                                         final String targetNamespace,
                                         final Collection<ResourceReference> references ) {
            this.goid = goid;
            this.version = version;
            this.uri = uri;
            this.targetNamespace = targetNamespace;
            this.references = Collections.unmodifiableCollection( new ArrayList<ResourceReference>(references) );
        }
    }

    private static final class TnsKey {
        private final String targetNamespace;

        private TnsKey( final String targetNamespace ) {
            this.targetNamespace = targetNamespace;
        }

        @SuppressWarnings({ "RedundantIfStatement" })
        @Override
        public boolean equals( final Object o ) {
            if ( this == o ) return true;
            if ( o == null || getClass() != o.getClass() ) return false;

            final TnsKey tnsKey = (TnsKey) o;

            if ( targetNamespace != null ? !targetNamespace.equals( tnsKey.targetNamespace ) : tnsKey.targetNamespace != null )
                return false;

            return true;
        }

        @Override
        public int hashCode() {
            return targetNamespace != null ? targetNamespace.hashCode() : 0;
        }
    }
}
