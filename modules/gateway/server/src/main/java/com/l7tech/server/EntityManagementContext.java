package com.l7tech.server;

import com.l7tech.objectmodel.GoidEntityManager;
import com.l7tech.util.Option;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Represents an Entity management context for a particular data source.
 *
 * <p>Users can access entity managers from the context.</p>
 */
public class EntityManagementContext {

    //- PUBLIC

    public EntityManagementContext( @NotNull final Set<? extends EntityManagerProvider> entityManagerProviders ) {
        this.entityManagerProviders = entityManagerProviders;
    }

    public <EM extends GoidEntityManager> EM getEntityManager( @NotNull final Class<EM> entityManager ) throws EntityManagerException {
        for ( final EntityManagerProvider provider : entityManagerProviders ) {
            final Option<EM> manager = provider.getEntityManager( entityManager );
            if ( manager.isSome() ) {
                return manager.some();
            }
        }
        throw new EntityManagerNotFoundException("Entity manager not found for type '"+entityManager.getName()+"'");
    }

    /**
     * Provider is responsible for managing instances and initializing/upgrading schemas, etc.
     */
    public interface EntityManagerProvider {
        /**
         * Get the entity manager for the given class
         *
         * <p>If the provider does not support the requested type of entity
         * manager then it should return <code>none</code></p>
         *
         * @param entityManager The manager class
         * @param <EM> The manager type
         * @return The entity manager or none if not supported by this provider
         * @throws EntityManagerException If an error occurred
         * @see Option#none
         */
        @NotNull
        <EM extends GoidEntityManager> Option<EM> getEntityManager( @NotNull Class<EM> entityManager ) throws EntityManagerException;
    }

    public static class EntityManagerException extends Exception {
        public EntityManagerException( final String message, final Throwable cause ) {
            super( message, cause );
        }

        public EntityManagerException( final String message ) {
            super( message );
        }
    }

    public static class EntityManagerNotFoundException extends EntityManagerException {
        public EntityManagerNotFoundException( final String message, final Throwable cause ) {
            super( message, cause );
        }

        public EntityManagerNotFoundException( final String message ) {
            super( message );
        }
    }

    //- PRIVATE

    private final Set<? extends EntityManagerProvider> entityManagerProviders;
}
