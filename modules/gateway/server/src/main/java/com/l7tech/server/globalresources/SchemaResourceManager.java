package com.l7tech.server.globalresources;

import com.l7tech.gateway.common.resources.ResourceEntry;
import com.l7tech.gateway.common.resources.ResourceEntryHeader;
import com.l7tech.gateway.common.resources.ResourceType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.communityschemas.SchemaHandle;
import com.l7tech.server.communityschemas.SchemaManager;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.util.Config;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.Triple;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.xml.sax.SAXException;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides the glue between the ResourceEntryManager and SchemaManager.
 */
public class SchemaResourceManager implements ApplicationListener, InitializingBean, PropertyChangeListener {

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

    @Override
    public void propertyChange( final PropertyChangeEvent evt ) {
        invalidationTime.set( System.currentTimeMillis() );
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        loadHardwareSchemas();
        timer.schedule( new TimerTask(){
            @Override
            public void run() {
                loadHardwareSchemasIfRequired();
            }
        }, 15731, 5331 );
    }

    @Override
    public void onApplicationEvent( final ApplicationEvent event ) {
        if ( event instanceof EntityInvalidationEvent ) {
            final EntityInvalidationEvent invalidationEvent = (EntityInvalidationEvent) event;
            if ( ResourceEntry.class.isAssignableFrom(invalidationEvent.getEntityClass()) ) {
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

    private final AtomicLong invalidationTime = new AtomicLong();
    private final Object hardwareSchemaLock = new Object();
    private Set<String> hardwareSchemas = Collections.emptySet();
    private long lastHardwareSchemaLoadTime;

    /**
     *
     */
    private void loadHardwareSchemasIfRequired() {
        final long invalidationTime = this.invalidationTime.get();
        final long lastRebuildTime;
        synchronized( hardwareSchemaLock ) {
            lastRebuildTime = lastHardwareSchemaLoadTime;
        }

        if (  lastRebuildTime < invalidationTime ) {
            loadHardwareSchemas();
        }
    }

    /**
     *
     */
    private void loadHardwareSchemas() {
        synchronized ( hardwareSchemaLock ) {
            lastHardwareSchemaLoadTime = System.currentTimeMillis();
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
                        handle = schemaManager.getSchemaByUri(  uri );
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

}
