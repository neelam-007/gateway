package com.l7tech.server.globalresources;

import com.l7tech.gateway.common.AsyncAdminMethods;
import com.l7tech.gateway.common.resources.*;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.objectmodel.*;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.admin.AsyncAdminMethodsImpl;
import com.l7tech.server.event.AdminInfo;
import com.l7tech.server.service.ServiceDocumentResolver;
import com.l7tech.util.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.logging.Logger;

/**
 * Resource administration implementation
 */
public class ResourceAdminImpl extends AsyncAdminMethodsImpl implements ResourceAdmin {

    //- PUBLIC

    public ResourceAdminImpl( final Config config,
                              final ResourceEntryManager resourceEntryManager,
                              final DefaultHttpProxyManager defaultHttpProxyManager,
                              final HttpConfigurationManager httpConfigurationManager,
                              final ServiceDocumentResolver serviceDocumentResolver,
                              final SchemaResourceManager schemaResourceManager,
                              final Map<String,String> defaultResourceMap ) {
        this.config = config;
        this.resourceEntryManager = resourceEntryManager;
        this.defaultHttpProxyManager = defaultHttpProxyManager;
        this.httpConfigurationManager = httpConfigurationManager;
        this.serviceDocumentResolver = serviceDocumentResolver;
        this.schemaResourceManager = schemaResourceManager;
        this.defaultResources = buildDefaultResources( defaultResourceMap );
    }

    @Override
    public Collection<ResourceEntryHeader> findAllResources() throws FindException {
        return resourceEntryManager.findAllHeaders();
    }

    @Override
    public ResourceEntry findResourceEntryByPrimaryKey( final Goid goid ) throws FindException {
        return resourceEntryManager.findByPrimaryKey( goid );
    }

    @Override
    public ResourceEntry findResourceEntryByUriAndType( final String uri, final ResourceType type ) throws FindException {
        return resourceEntryManager.findResourceByUriAndType( uri, type );
    }

    @Override
    public Goid saveResourceEntry( final ResourceEntry resourceEntry ) throws SaveException, UpdateException {
        Goid goid;

        if ( Goid.isDefault(resourceEntry.getGoid()) ) {
            goid = resourceEntryManager.save( resourceEntry );
        } else {
            goid = resourceEntry.getGoid();
            resourceEntryManager.update( resourceEntry );
        }

        return goid;
    }

    @Override
    public void saveResourceEntryBag( final ResourceEntryBag resourceEntryBag ) throws SaveException, UpdateException {
        for ( final ResourceEntry resourceEntry : resourceEntryBag ) {
            saveResourceEntry( resourceEntry );
        }
    }

    @Override
    public void deleteResourceEntry( final ResourceEntry resourceEntry ) throws DeleteException {
        if ( Goid.isDefault(resourceEntry.getGoid()) ) {
            throw new DeleteException( "Cannot delete, entity not persistent" );
        } 
        resourceEntryManager.delete( resourceEntry );
    }

    @Override
    public void deleteResourceEntry( final Goid resourceEntryGoid ) throws FindException, DeleteException {
        if ( Goid.isDefault(resourceEntryGoid) ) {
            throw new DeleteException( "Cannot delete, entity not persistent" );
        }
        resourceEntryManager.delete( resourceEntryGoid );
    }

    @Override
    public Collection<ResourceEntryHeader> findResourceHeadersByType( final ResourceType type ) throws FindException {
        return resourceEntryManager.findHeadersByType( type );
    }

    @Override
    public ResourceEntryHeader findResourceHeaderByUriAndType( final String uri, final ResourceType type ) throws FindException {
        return resourceEntryManager.findHeaderByUriAndType( uri, type );
    }

    @Override
    public Collection<ResourceEntryHeader> findResourceHeadersByKeyAndType( final String key, final ResourceType type ) throws FindException {
        return resourceEntryManager.findHeadersByKeyAndType( key, type );
    }

    @Override
    public Collection<ResourceEntryHeader> findResourceHeadersByTargetNamespace( final String targetNamespace ) throws FindException {
        return resourceEntryManager.findHeadersByTNS( targetNamespace );
    }

    @Override
    public Collection<ResourceEntryHeader> findResourceHeadersByPublicIdentifier( final String publicIdentifier ) throws FindException {
        return resourceEntryManager.findHeadersByPublicIdentifier( publicIdentifier );
    }

    @Override
    public ResourceEntry findDefaultResourceByUri( final String uri ) throws FindException {
        ResourceEntry resourceEntry = null;

        for ( final Map.Entry<String,ResourceEntryHeader> defaultResourceEntry : defaultResources.entrySet() ) {
            final ResourceEntryHeader resourceEntryHeader = defaultResourceEntry.getValue();
            if ( resourceEntryHeader.getUri().equals( uri ) ) {
                InputStream resourceIn = null;
                try {
                    resourceIn = ResourceAdminImpl.class.getResourceAsStream(defaultResourceEntry.getKey());
                    if ( resourceIn == null ) {
                        logger.warning( "Default resource not found '"+defaultResourceEntry.getKey()+"'." );
                        continue;
                    }

                    final byte[] resourceContent =
                            IOUtils.slurpStream( resourceIn, 1024*1024 );
                    
                    resourceEntry = new ResourceEntry();
                    resourceEntry.setType( resourceEntryHeader.getResourceType() );
                    resourceEntry.setContentType( resourceEntryHeader.getResourceType().getMimeType() );
                    resourceEntry.setUri( resourceEntryHeader.getUri() );
                    resourceEntry.setContent( new String( resourceContent, Charsets.UTF8 ) );
                    resourceEntry.setResourceKey1( resourceEntryHeader.getResourceKey1() );
                    resourceEntry.setResourceKey2( resourceEntryHeader.getResourceKey2() );
                    resourceEntry.setResourceKey3( resourceEntryHeader.getResourceKey3() );
                } catch ( IOException e ) {
                    throw new FindException("Error loading resource", e);
                } finally {
                    ResourceUtils.closeQuietly( resourceIn );
                }
                break;
            }
        }

        return resourceEntry;
    }

    @Override
    public Collection<ResourceEntryHeader> findDefaultResources() throws FindException {
        return new ArrayList<ResourceEntryHeader>(defaultResources.values());
    }

    @Override
    public int countRegisteredSchemas( final Collection<Goid> resourceGoids ) throws FindException {
        final Collection<String> uris = new ArrayList<String>();
        for ( final Goid goid : resourceGoids ) {
            if ( goid == null ) continue;
            final ResourceEntry entry = resourceEntryManager.findByPrimaryKey( goid );
            if ( entry != null ) {
                uris.add( entry.getUri() );
            }
        }
        
        int useCount = 0;
        for ( final String uri : uris ) {
            if ( schemaResourceManager.isSchemaRequired( uri ) ) {
                useCount++;
            }
        }

        return useCount;
    }

    @Override
    public boolean allowSchemaDoctype() {
        return config.getBooleanProperty( ServerConfigParams.PARAM_SCHEMA_ALLOW_DOCTYPE, false );
    }

    @Override
    public HttpProxyConfiguration getDefaultHttpProxyConfiguration() throws FindException {
        return defaultHttpProxyManager.getDefaultHttpProxyConfiguration();
    }

    @Override
    public void setDefaultHttpProxyConfiguration( final HttpProxyConfiguration httpProxyConfiguration ) throws SaveException, UpdateException {
        defaultHttpProxyManager.setDefaultHttpProxyConfiguration( httpProxyConfiguration );
    }

    @Override
    public Collection<HttpConfiguration> findAllHttpConfigurations() throws FindException {
        return httpConfigurationManager.findAll();
    }

    @Override
    public HttpConfiguration findHttpConfigurationByPrimaryKey( final Goid goid ) throws FindException {
        return httpConfigurationManager.findByPrimaryKey( goid );
    }

    @Override
    public void deleteHttpConfiguration( final HttpConfiguration httpConfiguration ) throws DeleteException {
        if ( HttpConfiguration.DEFAULT_GOID.equals(httpConfiguration.getGoid() ) ) {
            throw new DeleteException( "Cannot delete, entity not persistent" );
        }
        httpConfigurationManager.delete( httpConfiguration );
    }

    @Override
    public Goid saveHttpConfiguration( final HttpConfiguration httpConfiguration ) throws SaveException, UpdateException {
        final Goid goid;

        if ( HttpConfiguration.DEFAULT_GOID.equals(httpConfiguration.getGoid()) ) {
            goid = httpConfigurationManager.save( httpConfiguration );
        } else {
            goid = httpConfiguration.getGoid();
            httpConfigurationManager.update( httpConfiguration );
        }

        return goid;
    }

    @Override
    public String resolveResource( final String url ) throws IOException {
        if ( url == null ) {
            throw new IOException("null url");
        }

        // Guess the type, use the default if unknown
        final String lowerUrl = url.toLowerCase();
        ServiceAdmin.DownloadDocumentType type;
        if ( lowerUrl.endsWith( ".xsd" ) ) {
            type = ServiceAdmin.DownloadDocumentType.SCHEMA;
        } else if ( lowerUrl.endsWith( ".xsl" ) ) {
            type = ServiceAdmin.DownloadDocumentType.XSL;
        } else if ( lowerUrl.endsWith( ".wsdl" ) || url.endsWith( "?wsdl" ) ) {
            type = ServiceAdmin.DownloadDocumentType.WSDL;
        } else {
            type = ServiceAdmin.DownloadDocumentType.UNKNOWN;
        }

        return serviceDocumentResolver.resolveDocumentTarget(url, type);
    }

    @Override
    public AsyncAdminMethods.JobId<String> resolveResourceAsync(final String url) {
        final FutureTask<String> resolveUrlTask = new FutureTask<>(AdminInfo.find(false).wrapCallable(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return resolveResource(url);
            }
        }));
        Background.scheduleOneShot(new TimerTask() {
            @Override
            public void run() {
                resolveUrlTask.run();
            }
        }, 0L);
        return registerJob(resolveUrlTask, String.class);
    }
    
    //- PRIVATE

    private static final Logger logger = Logger.getLogger( ResourceAdminImpl.class.getName() );

    private final Config config;
    private final ResourceEntryManager resourceEntryManager;
    private final DefaultHttpProxyManager defaultHttpProxyManager;
    private final HttpConfigurationManager httpConfigurationManager;
    private final ServiceDocumentResolver serviceDocumentResolver;
    private final SchemaResourceManager schemaResourceManager;
    private final Map<String,ResourceEntryHeader> defaultResources;

    /**
     *
     */
    private static Map<String,ResourceEntryHeader> buildDefaultResources( final Map<String,String> defaultResourceMap ) {
        final Map<String,ResourceEntryHeader> resourceMap = new HashMap<String,ResourceEntryHeader>();

        for ( final Map.Entry<String,String> defaultResourceEntry : defaultResourceMap.entrySet() ) {
            ResourceType headerType = null;
            for ( final ResourceType type : ResourceType.values() ) {
                if ( defaultResourceEntry.getKey().endsWith( type.getFilenameSuffix() ) ) {
                    headerType = type;
                    break;
                }
            }

            if ( headerType == null ) {
                logger.warning( "Cannot determine type for default resource '"+defaultResourceEntry.getKey()+"'." );
                continue;
            }

            final String[] uriAndRefKey1 = defaultResourceEntry.getValue().split( "\\|" );
            if ( uriAndRefKey1.length > 2 ) {
                logger.warning( "Unexpected number of values for resource '"+defaultResourceEntry.getKey()+"'." );
            }

            final ResourceEntryHeader header = new ResourceEntryHeader(
                    Goid.toString(ResourceEntry.DEFAULT_GOID),
                    uriAndRefKey1[0],
                    null,
                    headerType,
                    uriAndRefKey1.length > 1 ? uriAndRefKey1[1] : null,
                    null,
                    null,
                    0,
                    null);

            resourceMap.put( defaultResourceEntry.getKey(), header );
        }

        return Collections.unmodifiableMap( resourceMap );
    }
}
