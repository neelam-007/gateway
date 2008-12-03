package com.l7tech.server.migration;

import com.l7tech.objectmodel.ReadOnlyEntityManager;
import com.l7tech.objectmodel.IdentityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SearchableEntityManager;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.EntityHeaderSet;
import com.l7tech.objectmodel.EntityTypeRegistry;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.Identity;
import com.l7tech.server.identity.IdentityProviderFactory;

import java.util.Collection;
import java.util.ArrayList;

/**
 * IdentityEntityManagerSupport provides a method to search Identity entities without knowledge of providers.
 */
public class IdentityEntityManager implements ReadOnlyEntityManager<Identity, IdentityHeader>, SearchableEntityManager<Identity, IdentityHeader> {

    //- PUBLIC

    public IdentityEntityManager( final Class<? extends Identity> entityClass,
                                  final IdentityProviderFactory identityProviderFactory ) {
        this.entityClass = entityClass;
        this.type = EntityTypeRegistry.getEntityType( entityClass );
        this.identityProviderFactory = identityProviderFactory;
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public Collection<IdentityHeader> findHeaders( final int offset, final int windowSize, final String filter) throws FindException {
        Collection<IdentityHeader> headers = new ArrayList<IdentityHeader>();

        String searchFilter = filter;
        if ( filter == null || filter.isEmpty() ) {
            searchFilter = "*";
        }

        for ( IdentityProvider provider : identityProviderFactory.findAllIdentityProviders() ) {
            EntityHeaderSet<IdentityHeader> identityHeaders = provider.search( new EntityType[]{type}, searchFilter );
            if ( headers.size() < windowSize ) {
                headers.addAll( new ArrayList<IdentityHeader>(identityHeaders).subList(0, Math.min( identityHeaders.size(), windowSize - headers.size() )) );
            }

            if ( headers.size() >= windowSize ) {
                break;
            }
        }

        return headers;
    }

    @Override
    public Identity findByPrimaryKey(final long oid) throws FindException {
        return null;
    }

    @Override
    public Collection<IdentityHeader> findAllHeaders() throws FindException {
        return findHeaders( 0, 50, null );
    }

    @Override
    public Collection<IdentityHeader> findAllHeaders(final int offset, final int windowSize) throws FindException {
        return findHeaders( offset, windowSize, null );
    }

    @Override
    public Collection<Identity> findAll() throws FindException {
        return null;
    }

    @Override
    public Class<? extends Identity> getImpClass() {
        return entityClass;
    }

    //- PRIVATE

    private final Class<? extends Identity> entityClass;
    private final EntityType type;
    private final IdentityProviderFactory identityProviderFactory;
}