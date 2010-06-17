package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.util.Functions;

/**
 * Holder for entity information related to an operation.
 */
class EntityContext {

    //- PUBLIC

    /**
     * Get the type of the current entity.
     *
     * @return The type or null.
     */
    public static EntityType getEntityType() {
        return doWithContext( new Functions.Unary<EntityType,EntityContext>(){
            @Override
            public EntityType call( final EntityContext entityContext ) {
                return entityContext.entityType;
            }
        } );
    }

    /**
     * Get the type name of the current entity.
     *
     * @return The type name or null.
     */
    public static String getEntityTypeName() {
        return doWithContext( new Functions.Unary<String,EntityContext>(){
            @Override
            public String call( final EntityContext entityContext ) {
                return entityContext.entityType == null ? null : entityContext.entityType.getName();
            }
        } );
    }

    /**
     * Get the identifier of the current entity.
     *
     * @return The identifier or null.
     */
    public static String getEntityId() {
        return doWithContext( new Functions.Unary<String,EntityContext>(){
            @Override
            public String call( final EntityContext entityContext ) {
                return entityContext.entityId;
            }
        } );
    }

    /**
     * Get the message for the current entity.
     *
     * @return The message or null.
     */
    public static String getMessage() {
        return doWithContext( new Functions.Unary<String,EntityContext>(){
            @Override
            public String call( final EntityContext entityContext ) {
                return entityContext.message;
            }
        } );
    }

    /**
     * Set an appropriate message for an entity that cannot be found.
     */
    public static void setNotFound() {
        getEntityContext().message = "Entity not found";
    }

    /**
     * Set the current entity information.
     *
     * @param entityType The type of the entity.
     * @param entityId The identifier for the entity.
     */
    public static void setEntityInfo( final EntityType entityType, final String entityId ) {
        final EntityContext entityContext = getEntityContext();
        entityContext.entityType = entityType;
        entityContext.entityId = entityId;
    }

    public static void reset() {
        entityContext.set( null );
    }

    //- PRIVATE

    private static final ThreadLocal<EntityContext> entityContext = new ThreadLocal<EntityContext>();

    private String entityId;
    private EntityType entityType;
    private String message;

    private static EntityContext getEntityContext() {
        EntityContext context = entityContext.get();
        if ( context == null ) {
            context = new EntityContext();
            entityContext.set( context );
        }
        return context;
    }

    private static <T> T doWithContext( final Functions.Unary<T,EntityContext> callback ) {
        final T value;
        final EntityContext context = entityContext.get();
        if ( context != null ) {
            value = callback.call( context );
        } else {
            value = null;
        }
        return value;
    }
}
