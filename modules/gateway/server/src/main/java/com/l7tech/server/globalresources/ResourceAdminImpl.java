package com.l7tech.server.globalresources;

import com.l7tech.gateway.common.resources.HttpConfiguration;
import com.l7tech.gateway.common.resources.HttpProxyConfiguration;
import com.l7tech.gateway.common.resources.ResourceAdmin;
import com.l7tech.gateway.common.resources.ResourceEntry;
import com.l7tech.gateway.common.resources.ResourceEntryBag;
import com.l7tech.gateway.common.resources.ResourceEntryHeader;
import com.l7tech.gateway.common.resources.ResourceType;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.communityschemas.SchemaManager;
import com.l7tech.server.service.ServiceDocumentResolver;
import com.l7tech.util.Config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Resource administration implementation
 */
public class ResourceAdminImpl implements ResourceAdmin {

    //- PUBLIC

    public ResourceAdminImpl( final Config config,
                              final ResourceEntryManager resourceEntryManager,
                              final DefaultHttpProxyManager defaultHttpProxyManager,
                              final HttpConfigurationManager httpConfigurationManager,
                              final ServiceDocumentResolver serviceDocumentResolver,
                              final SchemaManager schemaManager ) {
        this.config = config;
        this.resourceEntryManager = resourceEntryManager;
        this.defaultHttpProxyManager = defaultHttpProxyManager;
        this.httpConfigurationManager = httpConfigurationManager;
        this.serviceDocumentResolver = serviceDocumentResolver;
        this.schemaManager = schemaManager;
    }

    @Override
    public Collection<ResourceEntryHeader> findAllResources() throws FindException {
        return resourceEntryManager.findAllHeaders();
    }

    @Override
    public ResourceEntry findResourceEntryByPrimaryKey( final long oid ) throws FindException {
        return resourceEntryManager.findByPrimaryKey( oid );
    }

    @Override
    public ResourceEntry findResourceEntryByUriAndType( final String uri, final ResourceType type ) throws FindException {
        return resourceEntryManager.findResourceByUriAndType( uri, type );
    }

    @Override
    public long saveResourceEntry( final ResourceEntry resourceEntry ) throws SaveException, UpdateException {
        long oid;

        if ( resourceEntry.getOid() == ResourceEntry.DEFAULT_OID ) {
            oid = resourceEntryManager.save( resourceEntry );
        } else {
            oid = resourceEntry.getOid();
            resourceEntryManager.update( resourceEntry );
        }

        return oid;
    }

    @Override
    public void saveResourceEntryBag( final ResourceEntryBag resourceEntryBag ) throws SaveException, UpdateException {
        for ( final ResourceEntry resourceEntry : resourceEntryBag ) {
            saveResourceEntry( resourceEntry );
        }
    }

    @Override
    public void deleteResourceEntry( final ResourceEntry resourceEntry ) throws DeleteException {
        if ( resourceEntry.getOid() == ResourceEntry.DEFAULT_OID ) {
            throw new DeleteException( "Cannot delete, entity not persistent" );
        } 
        resourceEntryManager.delete( resourceEntry );
    }

    @Override
    public void deleteResourceEntry( final long resourceEntryOid ) throws FindException, DeleteException {
        if ( resourceEntryOid == ResourceEntry.DEFAULT_OID ) {
            throw new DeleteException( "Cannot delete, entity not persistent" );
        }
        resourceEntryManager.delete( resourceEntryOid );
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
    public Collection<ResourceEntryHeader> findResourceHeadersByTargetNamespace( final String targetNamespace ) throws FindException {
        return resourceEntryManager.findHeadersByTNS( targetNamespace );
    }

    @Override
    public Collection<ResourceEntryHeader> findResourceHeadersByPublicIdentifier( final String publicIdentifier ) throws FindException {
        return resourceEntryManager.findHeadersByPublicIdentifier( publicIdentifier );
    }

    @Override
    public int countRegisteredSchemas( final Collection<Long> resourceOids ) throws FindException {
        final Collection<String> uris = new ArrayList<String>();
        for ( final Long oid : resourceOids ) {
            if ( oid == null ) continue;
            final ResourceEntry entry = resourceEntryManager.findByPrimaryKey( oid );
            if ( entry != null ) {
                uris.add( entry.getUri() );
            }
        }
        
        int useCount = 0;
        for ( final String uri : uris ) {
            if ( schemaManager.isSchemaRegistered( uri ) ) {
                useCount++;
            }
        }

        return useCount;
    }

    @Override
    public boolean allowSchemaDoctype() {
        return config.getBooleanProperty( ServerConfig.PARAM_SCHEMA_ALLOW_DOCTYPE, false );
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
    public HttpConfiguration findHttpConfigurationByPrimaryKey( final long oid ) throws FindException {
        return httpConfigurationManager.findByPrimaryKey( oid );
    }

    @Override
    public void deleteHttpConfiguration( final HttpConfiguration httpConfiguration ) throws DeleteException {
        if ( httpConfiguration.getOid() == ResourceEntry.DEFAULT_OID ) {
            throw new DeleteException( "Cannot delete, entity not persistent" );
        }
        httpConfigurationManager.delete( httpConfiguration );
    }

    @Override
    public long saveHttpConfiguration( final HttpConfiguration httpConfiguration ) throws SaveException, UpdateException {
        final long oid;

        if ( httpConfiguration.getOid() == HttpConfiguration.DEFAULT_OID ) {
            oid = httpConfigurationManager.save( httpConfiguration );
        } else {
            oid = httpConfiguration.getOid();
            httpConfigurationManager.update( httpConfiguration );
        }

        return oid;
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
    
    //- PRIVATE

    private final Config config;
    private final ResourceEntryManager resourceEntryManager;
    private final DefaultHttpProxyManager defaultHttpProxyManager;
    private final HttpConfigurationManager httpConfigurationManager;
    private final ServiceDocumentResolver serviceDocumentResolver;
    private final SchemaManager schemaManager;
}
