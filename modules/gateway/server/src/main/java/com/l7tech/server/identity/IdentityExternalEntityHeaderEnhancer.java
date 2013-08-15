package com.l7tech.server.identity;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.ExternalEntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.IdentityHeader;
import com.l7tech.server.EntityCrud;
import com.l7tech.server.EntityHeaderUtils;
import com.l7tech.server.cluster.ExternalEntityHeaderEnhancer;

import javax.inject.Inject;

/**
 * ExternalEntityHeaderEnhancer that adds identity provider information to identities.
 */
public class IdentityExternalEntityHeaderEnhancer implements ExternalEntityHeaderEnhancer {

    @Inject
    private EntityCrud entityCrud;

    @Override
    public void enhance( final ExternalEntityHeader externalEntityHeader ) throws EnhancementException {
        if ( externalEntityHeader.getType() == EntityType.USER ||
             externalEntityHeader.getType() == EntityType.GROUP ) {
            try {
                final EntityHeader header = entityCrud.findHeader(
                        EntityType.ID_PROVIDER_CONFIG,
                        ((IdentityHeader) EntityHeaderUtils.fromExternal( externalEntityHeader )).getProviderGoid() );
                if ( header != null ) {
                    externalEntityHeader.setProperty("Scope Name", header.getName());
                }
            } catch ( FindException fe ) {
                throw new EnhancementException("Error loading the entity for header: " + externalEntityHeader, fe);
            }
        }

    }
}
