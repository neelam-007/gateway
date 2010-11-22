package com.l7tech.server.globalresources;

import com.l7tech.gateway.common.resources.ResourceEntry;
import com.l7tech.gateway.common.resources.ResourceEntryHeader;
import com.l7tech.gateway.common.resources.ResourceType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.EntityManagerStub;
import com.l7tech.util.Functions;

import java.util.Collection;

/**
 *
 */
public class ResourceEntryManagerStub extends EntityManagerStub<ResourceEntry, ResourceEntryHeader> implements ResourceEntryManager {

    public ResourceEntryManagerStub() {
    }

    public ResourceEntryManagerStub( final ResourceEntry... entitiesIn ) {
        super( entitiesIn );
    }

    @Override
    public ResourceEntry findResourceByUriAndType( final String uri, final ResourceType type ) throws FindException {
        return Functions.reduce( findAll(), null, new Functions.Binary<ResourceEntry,ResourceEntry,ResourceEntry>(){
            @Override
            public ResourceEntry call( final ResourceEntry currentMatch,
                                       final ResourceEntry resourceEntry ) {
                return currentMatch!=null ? currentMatch : uri.equals(resourceEntry.getUri())&&(type==null||type==resourceEntry.getType()) ? resourceEntry : null;
            }
        });
    }

    @Override
    public Collection<ResourceEntryHeader> findHeadersByType( final ResourceType type ) throws FindException {
        return Functions.grep( findAllHeaders(), new Functions.Unary<Boolean,ResourceEntryHeader>(){
            @Override
            public Boolean call( final ResourceEntryHeader resourceEntry ) {
                return resourceEntry.getResourceType() == type;
            }
        });
    }

    @Override
    public ResourceEntryHeader findHeaderByUriAndType( final String uri, final ResourceType type ) throws FindException {
        return Functions.reduce( findAllHeaders(), null, new Functions.Binary<ResourceEntryHeader,ResourceEntryHeader,ResourceEntryHeader>(){
            @Override
            public ResourceEntryHeader call( final ResourceEntryHeader currentMatch,
                                             final ResourceEntryHeader resourceEntry ) {
                return currentMatch!=null ? currentMatch : uri.equals(resourceEntry.getUri())&&(type==null||type==resourceEntry.getResourceType()) ? resourceEntry : null;
            }
        });
    }

    @Override
    public Collection<ResourceEntryHeader> findHeadersByTNS( final String targetNamespace ) throws FindException {
        return Functions.grep( findAllHeaders(), new Functions.Unary<Boolean,ResourceEntryHeader>(){
            @Override
            public Boolean call( final ResourceEntryHeader resourceEntry ) {
                return resourceEntry.getResourceType() == ResourceType.XML_SCHEMA &&
                       resourceEntry.getResourceKey1() != null &&
                       resourceEntry.getResourceKey1().equals( targetNamespace );
            }
        });
    }

    @Override
    public Collection<ResourceEntryHeader> findHeadersByPublicIdentifier( final String publicIdentifier ) throws FindException {
        return Functions.grep( findAllHeaders(), new Functions.Unary<Boolean,ResourceEntryHeader>(){
            @Override
            public Boolean call( final ResourceEntryHeader resourceEntry ) {
                return resourceEntry.getResourceType() == ResourceType.DTD &&
                       resourceEntry.getResourceKey1() != null && 
                       resourceEntry.getResourceKey1().equalsIgnoreCase( publicIdentifier );
            }
        });
    }

    @Override
    protected ResourceEntryHeader header( final ResourceEntry entity ) {
        return new ResourceEntryHeader( entity );
    }

    @Override
    public Class<ResourceEntry> getImpClass() {
        return ResourceEntry.class;
    }
}
