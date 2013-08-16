package com.l7tech.server.transport;

import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.gateway.common.transport.SsgActiveConnectorHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.HibernateGoidEntityManager;
import com.l7tech.server.util.MapRestriction;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Hibernate entity manager for SSG active connectors.
 */
public class SsgActiveConnectorManagerImpl
        extends HibernateGoidEntityManager<SsgActiveConnector, SsgActiveConnectorHeader>
        implements SsgActiveConnectorManager
{

    //- PUBLIC

    @NotNull
    @Override
    public Collection<SsgActiveConnector> findSsgActiveConnectorsByType( @NotNull final String type ) throws FindException {
       return findByPropertyMaybeNull( "type", type );
    }

    @Override
    public Class<SsgActiveConnector> getImpClass() {
        return SsgActiveConnector.class;
    }

    @Override
    public Collection<SsgActiveConnectorHeader> findHeaders( final int offset, final int windowSize, final Map<String, String> filters ) throws FindException {
        final Map<String,Object> connectorFilters = new HashMap<String, Object>(filters);

        String defaultFilter = filters.get(DEFAULT_SEARCH_NAME);
        if (defaultFilter != null && ! defaultFilter.isEmpty()) {
            connectorFilters.put("name", defaultFilter);
        }
        connectorFilters.remove(DEFAULT_SEARCH_NAME);

        // Criteria does not support collections (without a mapped entity) so use our own
        // critera for the restriction (supporting the usual map syntax "map['key']")
        for ( final Map.Entry<String,Object> entry : connectorFilters.entrySet() ) {
            if ( entry.getKey().startsWith( "properties['" ) && entry.getKey().endsWith( "']" ) ) {
                final String mapKey = entry.getKey().substring( 12, entry.getKey().length()-2 );
                final Object mapValue = entry.getValue();
                entry.setValue( propertyRestriction.containsEntry( mapKey, mapValue ) );
            }
        }

        return doFindHeaders( offset, windowSize, connectorFilters, false  );
    }

    //- PROTECTED

    @Override
    protected SsgActiveConnectorHeader newHeader( final SsgActiveConnector entity ) {
        final SsgActiveConnectorHeader header = new SsgActiveConnectorHeader(entity);
        header.setSecurityZoneGoid(entity.getSecurityZone() == null ? null : entity.getSecurityZone().getGoid());
        return header;
    }

    //- PRIVATE

    private MapRestriction propertyRestriction = new MapRestriction( SsgActiveConnector.class, "getProperties" );

}
