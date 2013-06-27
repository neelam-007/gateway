package com.l7tech.console.util;

import com.l7tech.objectmodel.GoidEntity;
import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.objectmodel.imp.NamedGoidEntityImp;

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
        entity.setName( getNameForCopy( entity.getName() ) );
    }

    /**
     * Configure an entity as a copy.
     *
     * @param entity The entity to update
     */
    public static void updateCopy( final NamedGoidEntityImp entity ) {
        resetIdentity( entity );
        entity.setName( getNameForCopy( entity.getName() ) );
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

    /**
     * Remove identity information from an entity.
     *
     * @param entity The entity to update.
     */
    public static void resetIdentity( final GoidEntity entity ) {
        entity.setGoid(GoidEntity.DEFAULT_GOID);
        entity.setVersion( 0 );
    }

    /**
     * Get a possibly updated name to use for a copied item.
     *
     * @param name The original name
     * @return The name to use for the copy
     */
    public static String getNameForCopy( final String name ) {
        String updatedName = name;
        if (  name != null && !name.startsWith( COPY_OF_PREFIX ) ) {
            updatedName = COPY_OF_PREFIX + name;
        }
        return updatedName;
    }
}
