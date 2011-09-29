package com.l7tech.console.util;

import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.objectmodel.imp.NamedEntityImp;

/**
 * Utility methods for entities
 */
public class EntityUtils {

    public static final String COPY_OF_PREFIX = "Copy of ";

    /**
     * Configure an entity as a copy.
     *
     * @param entity The entity to update
     */
    public static void updateCopy( final NamedEntityImp entity ) {
        resetIdentity( entity );
        if (  entity.getName() != null && !entity.getName().startsWith( COPY_OF_PREFIX ) ) {
            entity.setName( COPY_OF_PREFIX + entity.getName() );
        }
    }

    /**
     * Remove identity information from an entity.
     *
     * @param entity The entity to update.
     */
    public static void resetIdentity( final PersistentEntity entity ) {
        entity.setOid( PersistentEntity.DEFAULT_OID);
        entity.setVersion( 0 );
    }
}
