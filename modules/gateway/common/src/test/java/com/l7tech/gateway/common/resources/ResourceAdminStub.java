package com.l7tech.gateway.common.resources;

import com.l7tech.objectmodel.*;
import com.l7tech.util.Functions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * Stub for Resource Admin API
 *
 * <p>HTTP Configuration methods are not currently stubbed.</p>
 */
public class ResourceAdminStub implements ResourceAdmin {

    //- PUBLIC

    public ResourceAdminStub() {
        this( Collections.<ResourceEntry>emptyList() );        
    }

    public ResourceAdminStub( final Collection<ResourceEntry> resources ) {
        this.resources = new ArrayList<ResourceEntry>( resources );
    }

    public void setAllowSchemaDoctype( final boolean allowSchemaDoctype ) {
        this.allowSchemaDoctype = allowSchemaDoctype;
    }

    @Override
    public boolean allowSchemaDoctype() {
        return allowSchemaDoctype;
    }

    @Override
    public Collection<ResourceEntryHeader> findAllResources() throws FindException {
        return Functions.map( resources, new Functions.Unary<ResourceEntryHeader,ResourceEntry>(){
            @Override
            public ResourceEntryHeader call( final ResourceEntry resourceEntry ) {
                return new ResourceEntryHeader(resourceEntry);
            }
        } );
    }

    @Override
    public ResourceEntry findResourceEntryByPrimaryKey( final long oid ) throws FindException {
        return Functions.reduce( resources, null, new Functions.Binary<ResourceEntry,ResourceEntry,ResourceEntry>(){
            @Override
            public ResourceEntry call( final ResourceEntry resourceEntry, final ResourceEntry resourceEntry1 ) {
                return resourceEntry!=null ? resourceEntry : resourceEntry1.getOid()==oid ? resourceEntry1 : null;
            }
        } );
    }

    @Override
    public ResourceEntry findResourceEntryByUriAndType( final String uri, final ResourceType type ) throws FindException {
        return Functions.reduce( resources, null, new Functions.Binary<ResourceEntry,ResourceEntry,ResourceEntry>(){
            @Override
            public ResourceEntry call( final ResourceEntry resourceEntry, final ResourceEntry resourceEntry1 ) {
                return resourceEntry!=null ? resourceEntry : uri.equals(resourceEntry1.getUri()) && (type==null || type==resourceEntry1.getType()) ? resourceEntry1 : null;
            }
        } );
    }

    @Override
    public void deleteResourceEntry( final ResourceEntry resourceEntry ) throws DeleteException {
        resources.remove( resourceEntry );
    }

    @Override
    public void deleteResourceEntry( final long resourceEntryOid ) throws FindException, DeleteException {
        final ResourceEntry resourceEntry = findResourceEntryByPrimaryKey( resourceEntryOid );
        if (resourceEntry != null) resources.remove( resourceEntry );
    }

    @Override
    public long saveResourceEntry( final ResourceEntry resourceEntry ) throws SaveException, UpdateException {
        if ( resourceEntry.getOid()==ResourceEntry.DEFAULT_OID ) {
            final long nextId = Functions.reduce( resources, 0L, new Functions.Binary<Long,Long,ResourceEntry>(){
                @Override
                public Long call( final Long oid, final ResourceEntry resourceEntry1 ) {
                    return Math.max( oid, resourceEntry1.getOid() );
                }
            } );
            resourceEntry.setOid( nextId+1 );
        }
        resources.add( resourceEntry );
        return resourceEntry.getOid();
    }

    @Override
    public void saveResourceEntryBag( final ResourceEntryBag resourceEntryBag ) throws SaveException, UpdateException {
        for ( final ResourceEntry resourceEntry : resourceEntryBag ) {
            saveResourceEntry( resourceEntry );
        }
    }

    @Override
    public Collection<ResourceEntryHeader> findResourceHeadersByType( final ResourceType type ) throws FindException {
        return Functions.reduce( resources, new ArrayList<ResourceEntryHeader>(), new Functions.Binary<Collection<ResourceEntryHeader>,Collection<ResourceEntryHeader>,ResourceEntry>(){
            @Override
            public Collection<ResourceEntryHeader> call( final Collection<ResourceEntryHeader> resourceEntryHeaders,
                                                         final ResourceEntry resourceEntry ) {
                if ( resourceEntry.getType()==type ) {
                    resourceEntryHeaders.add( new ResourceEntryHeader(resourceEntry) );
                }
                return resourceEntryHeaders;
            }
        } );
    }

    @Override
    public ResourceEntryHeader findResourceHeaderByUriAndType( final String uri, final ResourceType type ) throws FindException {
        final ResourceEntry resourceEntry = findResourceEntryByUriAndType( uri, type );
        return resourceEntry == null ? null : new ResourceEntryHeader( resourceEntry );
    }

    @Override
    public Collection<ResourceEntryHeader> findResourceHeadersByKeyAndType( final String key, final ResourceType type ) throws FindException {
        return Functions.grep( findAllResources(), new Functions.Unary<Boolean,ResourceEntryHeader>(){
            @Override
            public Boolean call( final ResourceEntryHeader resourceEntryHeader ) {
                return ( type==null || resourceEntryHeader.getResourceType()==type ) &&
                        ( (key==null && resourceEntryHeader.getResourceKey1()==null) ||
                          (key!=null && key.equals( resourceEntryHeader.getResourceKey1() )));
            }
        } );
    }

    @Override
    public Collection<ResourceEntryHeader> findResourceHeadersByTargetNamespace( final String targetNamespace ) throws FindException {
        return Functions.grep( findAllResources(), new Functions.Unary<Boolean,ResourceEntryHeader>(){
            @Override
            public Boolean call( final ResourceEntryHeader resourceEntryHeader ) {
                return resourceEntryHeader.getResourceType()==ResourceType.XML_SCHEMA &&
                        ( (targetNamespace==null && resourceEntryHeader.getResourceKey1()==null) ||
                          (targetNamespace!=null && targetNamespace.equals( resourceEntryHeader.getResourceKey1() )));
            }
        } );
    }

    @Override
    public Collection<ResourceEntryHeader> findResourceHeadersByPublicIdentifier( final String publicIdentifier ) throws FindException {
        return Functions.grep( findAllResources(), new Functions.Unary<Boolean,ResourceEntryHeader>(){
            @Override
            public Boolean call( final ResourceEntryHeader resourceEntryHeader ) {
                return resourceEntryHeader.getResourceType()==ResourceType.DTD &&
                        (publicIdentifier!=null && publicIdentifier.equalsIgnoreCase( resourceEntryHeader.getResourceKey1() ));
            }
        } );
    }

    @Override
    public Collection<ResourceEntryHeader> findDefaultResources() throws FindException {
        return Collections.emptyList();
    }

    @Override
    public ResourceEntry findDefaultResourceByUri( final String uri ) throws FindException {
        return null;
    }

    @Override
    public int countRegisteredSchemas( final Collection<Long> resourceOids ) throws FindException {
        return 0;
    }

    @Override
    public HttpProxyConfiguration getDefaultHttpProxyConfiguration() throws FindException {
        return null;
    }

    @Override
    public void setDefaultHttpProxyConfiguration( final HttpProxyConfiguration httpProxyConfiguration ) throws SaveException, UpdateException {

    }

    @Override
    public Collection<HttpConfiguration> findAllHttpConfigurations() throws FindException {
        return null;
    }

    @Override
    public HttpConfiguration findHttpConfigurationByPrimaryKey( final Goid goid ) throws FindException {
        return null;
    }

    @Override
    public void deleteHttpConfiguration( final HttpConfiguration httpConfiguration ) throws DeleteException {
    }

    @Override
    public Goid saveHttpConfiguration( final HttpConfiguration httpConfiguration ) throws SaveException, UpdateException {
        return new Goid(0, 0);
    }

    public void setResolver( final Functions.UnaryThrows<String,String,IOException> resolver ) {
        this.resolver = resolver;
    }

    @Override
    public String resolveResource( final String url ) throws IOException {
        return resolver.call( url );
    }

    //- PRIVATE

    private final Collection<ResourceEntry> resources;
    private boolean allowSchemaDoctype;
    private Functions.UnaryThrows<String,String,IOException> resolver = new Functions.UnaryThrows<String,String,IOException>(){
        @Override
        public String call( final String s ) throws IOException {
            throw new IOException("Cannot resolve uri : " + s);
        }
    };

}
