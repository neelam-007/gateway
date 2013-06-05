package com.l7tech.server.globalresources;

import com.l7tech.gateway.common.resources.ResourceEntry;
import com.l7tech.gateway.common.resources.ResourceEntryHeader;
import com.l7tech.gateway.common.resources.ResourceType;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.HibernateEntityManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Resource entry manager implementation.
 */
@Transactional(propagation=Propagation.SUPPORTS, rollbackFor=Throwable.class)
public class ResourceEntryManagerImpl extends HibernateEntityManager<ResourceEntry, ResourceEntryHeader>  implements ResourceEntryManager
{
    //- PUBLIC


    @Override
    public Collection<ResourceEntryHeader> findHeaders( final int offset, final int windowSize, final Map<String, String> filters ) throws FindException {
        Map<String,Object> resourceEntryFilters = new HashMap<String,Object>(filters);
        String defaultFilter = filters.get(DEFAULT_SEARCH_NAME);
        if (defaultFilter != null && ! defaultFilter.isEmpty()) {
            resourceEntryFilters.put("uri", defaultFilter);
        }
        if ( filters.containsKey( "type" ) ) {
            resourceEntryFilters.put( "type", ResourceType.valueOf(filters.get("type")));
        }
        resourceEntryFilters.remove(DEFAULT_SEARCH_NAME);
        return doFindHeaders( offset, windowSize, resourceEntryFilters, false ); // conjunction
    }

    @Override
    public ResourceEntry findResourceByUriAndType( final String uri, final ResourceType type ) throws FindException {
        final Map<String,Object> criteria = new HashMap<String,Object>();
        criteria.put( "uri", uri );
        criteria.put( "type", type );
        return findUnique( criteria );
    }

    @Override
    public Collection<ResourceEntryHeader> findHeadersByType( final ResourceType type ) throws FindException {
        return findHeadersByResourceKey( type, null );
    }

    @Override
    public ResourceEntryHeader findHeaderByUriAndType( final String uri,
                                                       final ResourceType type ) throws FindException {
        return header( findResourceByUriAndType( uri, type ) );
    }

    @Override
    public Collection<ResourceEntryHeader> findHeadersByTNS( final String targetNamespace ) throws FindException {
        return findHeadersByResourceKey( ResourceType.XML_SCHEMA, targetNamespace==null ? NULL : targetNamespace );
    }

    @Override
    public Collection<ResourceEntryHeader> findHeadersByPublicIdentifier( final String publicIdentifier ) throws FindException {
        return findHeadersByResourceKey( ResourceType.DTD, publicIdentifier==null ? NULL : publicIdentifier );
    }

    @Override
    public Collection<ResourceEntryHeader> findHeadersByKeyAndType( final String key, final ResourceType type ) throws FindException {
        return findHeadersByResourceKey( type, key==null ? NULL : key );
    }

    @Override
    public Class<? extends Entity> getImpClass() {
        return ResourceEntry.class;
    }

    //- PROTECTED

    @Override
    protected UniqueType getUniqueType() {
        return UniqueType.OTHER;
    }

    @Override
    protected Collection<Map<String, Object>> getUniqueConstraints( final ResourceEntry resourceEntry ) {
        final Map<String,Object> map = new HashMap<String, Object>();
        map.put( "uri", resourceEntry.getUri() );
        return Arrays.asList( map );
    }

    @Override
    protected ResourceEntryHeader newHeader( final ResourceEntry entity ) {
        final ResourceEntryHeader header = new ResourceEntryHeader(entity);
        header.setSecurityZoneOid(entity.getSecurityZone() == null ? null : entity.getSecurityZone().getOid());
        return header;
    }

    //- PRIVATE

    private Collection<ResourceEntryHeader> findHeadersByResourceKey( final ResourceType type,
                                                                      final Object resourceKey ) throws FindException {
        final Map<String,Object> criteria = new HashMap<String,Object>();
        criteria.put( "type", type );
        criteria.put( "resourceKey1", resourceKey );
        return findMatchingHeaders( Collections.singleton( criteria ) );
    }

    private ResourceEntryHeader header( final ResourceEntry resourceEntry ) {
        ResourceEntryHeader header = null;
        if ( resourceEntry != null ) {
            header = newHeader( resourceEntry );    
        }
        return header;
    }
}
