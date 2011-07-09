package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.gateway.api.ManagedObject;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.NamedEntity;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.objectmodel.RoleAwareEntityManager;
import com.l7tech.objectmodel.StaleUpdateException;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.security.rbac.SecurityFilter;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * ResourceFactory implementation that uses an EntityManager to access Entity resources.
 * 
 * <p>If the factory is read only then methods that update state should not be used.</p>
 *
 * <p>RBAC checks are performed by this class for the standard CRUD methods.
 * Custom methods should call <code>checkPermitted</code> or <code>accessFilter</code>
 * to enforce access controls.</p>
 */
abstract class EntityManagerResourceFactory<R, E extends PersistentEntity, EH extends EntityHeader> extends ResourceFactorySupport<R> {

    //- PUBLIC

    @Override
    public final EntityType getType() {
        return EntityType.findTypeByEntity(manager.getImpClass());
    }

    @Override
    public boolean isReadOnly() {
        return readOnly;
    }

    @Override
    public Set<String> getSelectors() {
        if ( !allowNameSelection ) {
            return Collections.unmodifiableSet(new HashSet<String>(Arrays.asList( IDENTITY_SELECTOR, VERSION_SELECTOR )));
        } else {
            return Collections.unmodifiableSet(new HashSet<String>(Arrays.asList( IDENTITY_SELECTOR, NAME_SELECTOR, VERSION_SELECTOR )));
        }
    }

    @Override
    public Map<String, String> createResource( final Object resource ) throws InvalidResourceException {
        checkReadOnly();
        
        final long id = transactional( new TransactionalCallback<Long,InvalidResourceException>(){
            @SuppressWarnings({ "unchecked" })
            @Override
            public Long execute() throws ObjectModelException, InvalidResourceException {
                final EntityBag<E> entityBag = fromResourceAsBag( resource );
                for ( PersistentEntity entity : entityBag ) {
                    if ( entity.getOid() != PersistentEntity.DEFAULT_OID ||
                         (entity.getVersion() != 0 && entity.getVersion() != 1) ) { // some entities initialize the version to 1
                        throw new InvalidResourceException(InvalidResourceException.ExceptionType.INVALID_VALUES, "invalid identity or version");
                    }
                }

                checkPermitted( OperationType.CREATE, null, entityBag.getEntity() );

                beforeCreateEntity( entityBag );

                for ( PersistentEntity entity : entityBag ) {
                    validate( entity );                    
                }

                final long id = manager.save( entityBag.getEntity() );
                afterCreateEntity( entityBag, id );

                if ( manager instanceof RoleAwareEntityManager ) {
                    ((RoleAwareEntityManager<E>)manager).createRoles( entityBag.getEntity() );
                }

                EntityContext.setEntityInfo( getType(), Long.toString(id) );                

                return id;
            }
        }, false, InvalidResourceException.class );

        return Collections.singletonMap( IDENTITY_SELECTOR, Long.toString(id) );
    }

    @Override
    public R getResource( final Map<String, String> selectorMap ) throws ResourceNotFoundException {
        return transactional( new TransactionalCallback<R,ResourceNotFoundException>(){
            @Override
            public R execute() throws ObjectModelException, ResourceNotFoundException {
                EntityBag<E> entityBag = selectEntityBag(selectorMap);
                checkPermitted( OperationType.READ, null, entityBag.getEntity() );
                R resource = asResource( entityBag );
                if ( resource instanceof ManagedObject ) {
                    ManagedObject managedResource = (ManagedObject) resource;
                    managedResource.setId( entityBag.getEntity().getId() );
                    managedResource.setVersion( entityBag.getEntity().getVersion() );
                }
                return resource;
            }
        }, true, ResourceNotFoundException.class );
    }

    @Override
    public Collection<Map<String, String>> getResources() {
        Collection<Map<String,String>> resources = Collections.emptyList();

        try {
            Collection<EH> headers = manager.findAllHeaders();
            headers = accessFilter(headers, manager.getEntityType(), OperationType.READ, null);
            headers = filterHeaders( headers );

            resources = new ArrayList<Map<String,String>>( headers.size() );

            for ( EntityHeader header : headers ) {
                resources.add( Collections.singletonMap( IDENTITY_SELECTOR, header.getStrId() ) );
            }
        } catch (FindException e) {
            handleObjectModelException(e);
        }

        return resources;
    }

    @Override
    public R putResource( final Map<String, String> selectorMap, final Object resource ) throws ResourceNotFoundException, InvalidResourceException {
        checkReadOnly();

        final String id = transactional( new TransactionalCallback<String, Exception>(){
            @Override
            public String execute() throws ObjectModelException, ResourceNotFoundException, InvalidResourceException {
                final EntityBag<E> oldEntityBag = selectEntityBag( selectorMap );
                final EntityBag<E> newEntityBag = fromResourceAsBag( resource );

                if ( resource instanceof ManagedObject ) {
                    final ManagedObject managedResource = (ManagedObject) resource;
                    setIdentifier( newEntityBag.getEntity(), managedResource.getId() );
                    setVersion( newEntityBag.getEntity(), managedResource.getVersion() );
                }

                updateEntityBag( oldEntityBag, newEntityBag );

                if ( oldEntityBag.getEntity().getOid() != PersistentEntity.DEFAULT_OID &&
                     oldEntityBag.getEntity().getOid() != newEntityBag.getEntity().getOid() ) {
                    throw new InvalidResourceException(InvalidResourceException.ExceptionType.INVALID_VALUES, "identifier mismatch");
                }

                if ( oldEntityBag.getEntity().getVersion() != newEntityBag.getEntity().getVersion() ) {
                    throw new StaleUpdateException();
                }

                checkPermitted( OperationType.UPDATE, null, newEntityBag.getEntity() );

                beforeUpdateEntity( oldEntityBag );

                for ( PersistentEntity entity : oldEntityBag ) {
                    validate( entity );
                }

                manager.update( oldEntityBag.getEntity() );
                afterUpdateEntity( oldEntityBag );

                return oldEntityBag.getEntity().getId();
            }
        }, false, ResourceNotFoundException.class, InvalidResourceException.class);

        return getResource( Collections.singletonMap( IDENTITY_SELECTOR, id )); // re-select to get updated version#
    }

    @Override
    public String deleteResource( final Map<String, String> selectorMap ) throws ResourceNotFoundException {
        checkReadOnly();

        return transactional( new TransactionalCallback<String,ResourceNotFoundException>(){
            @SuppressWarnings({ "unchecked" })
            @Override
            public String execute() throws ObjectModelException, ResourceNotFoundException {
                final EntityBag<E> entityBag = selectEntityBag( selectorMap );

                checkPermitted( OperationType.DELETE, null, entityBag.getEntity() );

                beforeDeleteEntity( entityBag );
                manager.delete( entityBag.getEntity() );
                if ( manager instanceof RoleAwareEntityManager ) {
                    ((RoleAwareEntityManager<E>)manager).deleteRoles( entityBag.getEntity().getOid() );
                }
                afterDeleteEntity( entityBag );

                return entityBag.getEntity().getId();
            }
        }, false, ResourceNotFoundException.class);
    }

    //- PROTECTED

    /**
     * Convert the given entity to a resource.
     *
     * <p>This method may be invoked in a read-only transaction.</p>
     *
     * <p>If the resource is a {@code ManagedObject} it is not necessary to set
     * the identity and version of the resource.<p>
     *
     * <p>This implementation always throws.</p>
     *
     * @param entity The entity
     * @return The resource representation of the entity
     */
    protected R asResource( final E entity ) {
        throw new ResourceAccessException("Not implemented");
    }

    /**
     * Convert the given entities to a resource.
     *
     * <p>This method may be invoked in a read-only transaction.</p>
     *
     * <p>If the resource is a {@code ManagedObject} it is not necessary to set
     * the identity and version of the resource.<p>
     *
     * <p>This implementation calls {@code asResource} for the primary entity}.</p>
     *
     * @param entityBag The entity bag
     * @return The resource representation of the entity
     */
    protected R asResource( final EntityBag<E> entityBag ) {
        return asResource( entityBag.getEntity() );
    }

    /**
     * Convert the given resource to an entity.
     *
     * <p>This method should be overridden in factories that support updates,
     * and update only a single entity.</p>
     *
     * <p>This implementation always throws <code>InvalidResourceException</code></p>
     *
     * <p>If the resource is a {@code ManagedObject} it is not necessary to set
     * the identity and version of the entity.<p>
     *
     * @param resource The resource representation
     * @return The entity
     * @throws InvalidResourceException If the resource cannot be converted
     */
    protected E fromResource( Object resource ) throws InvalidResourceException {
        throw new InvalidResourceException(InvalidResourceException.ExceptionType.UNEXPECTED_TYPE, "update not supported");
    }

    /**
     * Convert the given resource to entities.
     *
     * <p>This method should be overridden in factories that support updates,
     * and update multiple entities.</p>
     *
     * <p>This implementation calls <code>fromResource</code></p>
     *
     * <p>If the main resource is a {@code ManagedObject} it is not necessary to set
     * the identity and version of the entity.<p>
     *
     * @param resource The resource representation
     * @return The entity
     * @throws InvalidResourceException If the resource cannot be converted
     */
    protected EntityBag<E> fromResourceAsBag( Object resource ) throws InvalidResourceException {
       return new EntityBag<E>( fromResource( resource ) );
    }

    /**
     * Override to filter access to the entity headers.
     *
     * @param headers The headers to filter.
     * @return The filtered collection.
     */
    protected Collection<EH> filterHeaders( final Collection<EH> headers ) {
        return headers;
    }

    /**
     * Override to filter access to the entity.
     *
     * @param entity The entity to check access for.
     * @return The entity if access is permitted, else null
     */
    protected E filterEntity( final E entity ) {
        return entity;
    }

    /**
     * Select an entity using the provided selector map.
     *
     * <p>The combination of all selectors must match the entity.</p>
     *
     * @param selectorMap The selectors to use.
     * @return The entity (never null)
     * @throws ResourceNotFoundException If the selectors do not identify an entity.
     */
    protected final E selectEntity( final Map<String, String> selectorMap ) throws ResourceNotFoundException {
        E entity = null;
        final String id = selectorMap.get( IDENTITY_SELECTOR );
        final String name = selectorMap.get( NAME_SELECTOR );
        final String version = selectorMap.get( VERSION_SELECTOR );

        if ( id == null && name == null ) {
            throw new InvalidResourceSelectors();
        }

        if ( id != null ) {
            try {
                entity = manager.findByPrimaryKey( toInternalId(id) );
            } catch (FindException e) {
                handleObjectModelException(e);
            }
        }

        if ( entity == null && name != null ) {
            try {
                entity = manager.findByUniqueName( name );
            } catch (FindException e) {
                handleObjectModelException(e);
            }
        }

        // Verify all selectors match (selectors must be AND'd)
        if ( entity != null ) {
            if ( id != null && !id.equalsIgnoreCase(entity.getId())) {
                entity = null;
            } else if ( name != null && entity instanceof NamedEntity && !name.equalsIgnoreCase(((NamedEntity)entity).getName())) {
                entity = null;
            } else if (version != null && !version.equals(Integer.toString(entity.getVersion())) ) {
                entity = null;
            }
        }

        if ( entity != null ) {
            entity = filterEntity( entity );
        }

        if ( entity == null ) {
            throw new ResourceNotFoundException("Resource not found " + selectorMap);
        } else {
            EntityContext.setEntityInfo( getType(), entity.getId() );
        }

        return entity;
    }

    /**
     * Select an entity and dependencies using the provided selector map.
     *
     * <p>The combination of all selectors must match the entity.</p>
     *
     * <p>This will call {@code loadEntityBag} to create a bag containing
     * dependencies.</p>
     *
     * @param selectorMap The selectors to use.
     * @return The entity (never null)
     * @throws ResourceNotFoundException If the selectors do not identify an entity.
     * @throws ObjectModelException If a persistence error occurs
     */
    protected final EntityBag<E> selectEntityBag( final Map<String, String> selectorMap ) throws ResourceNotFoundException, ObjectModelException {
        return loadEntityBag( selectEntity( selectorMap ) );
    }

    /**
     * Load the dependencies of the given entity into an EntityBag.
     *
     * <p>This implementation creates an EntityBag with the primary entity.</p>
     *
     * @param entity The entity
     * @return The bag containing all entities.
     * @throws ObjectModelException If a persistence error occurs
     */
    protected EntityBag<E> loadEntityBag( final E entity ) throws ObjectModelException {
        return new EntityBag<E>( entity );
    }

    /**
     * Update the existing entity with values from the new entity.
     *
     * <p>This method should be overridden in factories that support updates
     * of a single entity.</p>
     *
     * <p>Implementers do not need to check the identity of the entities or to
     * update version information.</p>
     *
     * <p>Any data that is read only should not be copied to the existing
     * entity.</p>
     *
     * <p>This implementation always throws <code>InvalidResourceException</code></p>
     *
     * @param oldEntity The existing entity
     * @param newEntity The updated entity
     * @throws InvalidResourceException If the updated entity is not valid
     */
    protected void updateEntity( final E oldEntity, final E newEntity ) throws InvalidResourceException {
        throw new InvalidResourceException(InvalidResourceException.ExceptionType.UNEXPECTED_TYPE, "update not supported");
    }

    /**
     * Update the existing entities with values from the new entities.
     *
     * <p>This method should be overridden in factories that support updates
     * of multiple entities.</p>
     *
     * <p>Implementers do not need to check the identity of the primary entity
     * or to update version information for the primary entity.</p>
     *
     * <p>Any data that is read only should not be copied to the existing
     * entities.</p>
     *
     * <p>This implementation calls updateEntity for the primary entity.</p>
     *
     * @param oldEntityBag The existing entities
     * @param newEntityBag The updated entities
     * @throws InvalidResourceException If the updated entity is not valid
     */
    protected void updateEntityBag( final EntityBag<E> oldEntityBag, final EntityBag<E> newEntityBag ) throws InvalidResourceException {
        updateEntity( oldEntityBag.getEntity(), newEntityBag.getEntity() );
    }

    /**
     * Entity creation callback before the primary entity is persisted.
     *
     * <p>This implementation does nothing.</p>
     *
     * @param entityBag The entityBag.
     * @throws ObjectModelException If an error occurs
     */
    protected void beforeCreateEntity( final EntityBag<E> entityBag ) throws ObjectModelException {}

    /**
     * Entity creation callback after the primary entity is persisted.
     *
     * <p>This implementation does nothing.</p>
     *
     * @param entityBag The entityBag.
     * @param identifier The identifier for the new entity.
     * @throws ObjectModelException If an error occurs
     */
    protected void afterCreateEntity( final EntityBag<E> entityBag, long identifier ) throws ObjectModelException {}

    /**
     * Entity update callback before the primary entity is persisted.
     *
     * <p>This implementation does nothing.</p>
     *
     * @param entityBag The entityBag.
     * @throws ObjectModelException If an error occurs
     */
    @SuppressWarnings({ "UnusedDeclaration" })
    protected void beforeUpdateEntity( final EntityBag<E> entityBag ) throws ObjectModelException {}

    /**
     * Entity update callback after the primary entity is persisted.
     *
     * <p>This implementation does nothing.</p>
     *
     * @param entityBag The entityBag.
     * @throws ObjectModelException If an error occurs
     */
    protected void afterUpdateEntity( final EntityBag<E> entityBag ) throws ObjectModelException {}

    /**
     * Entity deletion callback before the primary entity is persisted.
     *
     * <p>This implementation does nothing.</p>
     *
     * @param entityBag The entityBag.
     * @throws ObjectModelException If an error occurs
     */
    @SuppressWarnings({ "UnusedDeclaration" })
    protected void beforeDeleteEntity( final EntityBag<E> entityBag ) throws ObjectModelException {}

    /**
     * Entity deletion callback after the primary entity is persisted.
     *
     * <p>This implementation does nothing.</p>
     *
     * @param entityBag The entityBag.
     * @throws ObjectModelException If an error occurs
     */
    protected void afterDeleteEntity( final EntityBag<E> entityBag ) throws ObjectModelException {}

    /**
     * Convert the given identifier to the internal <code>long</code> format.
     *
     * @param identifier The identifier to process.
     * @return The identifier as a long
     * @throws InvalidResourceSelectors If the given identifier is not valid
     */
    protected final long toInternalId( final String identifier ) throws InvalidResourceSelectors {
        if ( identifier == null ) throw new InvalidResourceSelectors();
        try {
            return Long.parseLong(identifier);
        } catch ( NumberFormatException nfe ) {
            throw new InvalidResourceSelectors();            
        }
    }

    /**
     * Convert the given identifier to the internal <code>long</code> format.
     *
     * @param identifier The identifier to process.
     * @param identifierDescription A user facing description of the identifier.
     * @return The identifier as a long
     * @throws InvalidResourceException If the given identifier is not valid
     */
    protected final long toInternalId( final String identifier,
                                       final String identifierDescription ) throws InvalidResourceException {
        if ( identifier == null )
            throw new InvalidResourceException(
                    InvalidResourceException.ExceptionType.MISSING_VALUES,
                    "Missing " + identifierDescription );
        try {
            return Long.parseLong(identifier);
        } catch ( NumberFormatException nfe ) {
            throw new InvalidResourceException(
                    InvalidResourceException.ExceptionType.INVALID_VALUES,
                    "Invalid " + identifierDescription );
        }
    }

    /**
     * Set the identifier for the entity from the given resource identifier.
     *
     * @param entity The target entity
     * @param identifier The resource identifier
     * @throws InvalidResourceException If the given identifier is not valid
     */
    protected final void setIdentifier( final PersistentEntity entity,
                                        final String identifier ) throws InvalidResourceException {
        setIdentifier( entity, identifier, true );
    }

    /**
     * Set the identifier for the entity from the given resource identifier.
     *
     * @param entity The target entity
     * @param identifier The resource identifier
     * @param required Is the identifier required
     * @throws InvalidResourceException If the given identifier is not valid
     */
    protected final void setIdentifier( final PersistentEntity entity,
                                        final String identifier,
                                        final boolean required ) throws InvalidResourceException {
        if ( identifier!=null || required ) {
            try {
                entity.setOid( toInternalId( identifier ) );
            } catch (InvalidResourceSelectors invalidResourceSelectors) {
                throw new InvalidResourceException(InvalidResourceException.ExceptionType.INVALID_VALUES, "invalid identifier");
            }
        }
    }

    /**
     * Set the version for then entity from the given resource version.
     *
     * @param entity The target entity
     * @param version The resource identifier
     * @throws InvalidResourceException If the given identifier is not valid
     */
    protected final void setVersion( final PersistentEntity entity,
                                     final Integer version ) throws InvalidResourceException {
        setVersion( entity, version, true );
    }

    /**
     * Set the version for then entity from the given resource version.
     *
     * @param entity The target entity
     * @param version The resource identifier
     * @param required Is the identifier required
     * @throws InvalidResourceException If the given identifier is not valid
     */
    protected final void setVersion( final PersistentEntity entity,
                                     final Integer version,
                                     final boolean required ) throws InvalidResourceException {
        if ( version != null ) {
            entity.setVersion( version );
        } else if ( required ) {
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.MISSING_VALUES, "missing version");
        }
    }

    /**
     * Cast the given object to the specified type
     *
     * @param object The object to cast
     * @param typeClass The target type.
     * @return The object as the desired type
     */
    @SuppressWarnings({ "unchecked" })
    protected final <T> T cast( Object object, Class<T> typeClass ) {
        if ( !typeClass.isInstance( object ) ) throw new ResourceAccessException("Invalid type");
        return (T) object;
    }

    protected static class EntityBag<BE extends PersistentEntity> implements Iterable<PersistentEntity> {
        private final BE entity;

        protected EntityBag( final BE entity ) {
            this.entity = entity;
        }

        /**
         * Access the primary entity. 
         */
        protected BE getEntity() {
            return entity;
        }

        @Override
        public Iterator<PersistentEntity> iterator() {
            return Collections.<PersistentEntity>singletonList( entity ).iterator();
        }
    }

    //- PACKAGE

    static final String IDENTITY_SELECTOR = "id";
    static final String NAME_SELECTOR = "name";
    static final String VERSION_SELECTOR = "version";

    EntityManagerResourceFactory( final boolean readOnly,
                                  final boolean allowNameSelection,
                                  final RbacServices rbacServices,
                                  final SecurityFilter securityFilter,
                                  final PlatformTransactionManager transactionManager,
                                  final EntityManager<E,EH> manager ) {
        super( rbacServices, securityFilter, transactionManager );
        this.readOnly = readOnly;
        this.allowNameSelection = allowNameSelection;
        this.manager = manager;
    }

    //- PRIVATE

    private final boolean readOnly;
    private final boolean allowNameSelection;
    private final EntityManager<E,EH> manager;

    private void checkReadOnly() {
        if ( isReadOnly() ) throw new IllegalStateException("Read only");
    }

}
