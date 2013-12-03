package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.gateway.api.ManagedObject;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.PermissionDeniedException;
import com.l7tech.objectmodel.*;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.security.rbac.SecurityFilter;
import com.l7tech.util.*;
import com.l7tech.util.Eithers.E2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.PlatformTransactionManager;

import java.lang.reflect.Method;
import java.util.*;

import static com.l7tech.util.Either.left;
import static com.l7tech.util.Either.right;
import static com.l7tech.util.Option.optional;

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
        final HashSet<String> baseSelectors = new HashSet<String>(Arrays.asList(IDENTITY_SELECTOR, VERSION_SELECTOR));
        if ( allowNameSelection ) {
            baseSelectors.add(NAME_SELECTOR);
        }
        if ( allowGuidSelection ) {
            baseSelectors.add(GUID_SELECTOR);
        }

        baseSelectors.addAll(getCustomSelectors());
        return Collections.unmodifiableSet(baseSelectors);
    }

    @Override
    public Map<String, String> createResource( final Object resource ) throws InvalidResourceException {
        return createResource(null, resource);
    }

    @Override
    public Map<String, String> createResource( @Nullable final String id, final Object resource ) throws InvalidResourceException {
        checkReadOnly();

        final Goid goid = Eithers.extract(transactional(new TransactionalCallback<Either<InvalidResourceException, Goid>>() {
            @SuppressWarnings({"unchecked"})
            @Override
            public Either<InvalidResourceException, Goid> execute() throws ObjectModelException {
                try {
                    final EntityBag<E> entityBag = fromResourceAsBag(resource);
                    for (PersistentEntity entity : entityBag) {
                        if (entity.getVersion() == VERSION_NOT_PRESENT) {
                            entity.setVersion(0);
                        }

                        if (!entity.getGoid().equals(PersistentEntity.DEFAULT_GOID) ||
                                (entity.getVersion() != 0 && entity.getVersion() != 1)) { // some entities initialize the version to 1
                            throw new InvalidResourceException(InvalidResourceException.ExceptionType.INVALID_VALUES, "invalid identity or version");
                        }
                    }

                    checkPermitted(OperationType.CREATE, null, entityBag.getEntity());

                    beforeCreateEntity(entityBag);

                    for (PersistentEntity entity : entityBag) {
                        validate(entity);
                    }

                    final Goid goid;
                    if(id != null){
                        goid = Goid.parseGoid(id);
                        doSaveEntity(goid, entityBag.getEntity());
                    } else {
                        goid = doSaveEntity(entityBag.getEntity());
                    }
                    afterCreateEntity(entityBag, goid);

                    if (manager instanceof RoleAwareEntityManager) {
                        ((RoleAwareEntityManager<E>) manager).createRoles(entityBag.getEntity());
                    }

                    EntityContext.setEntityInfo(getType(), goid.toString());

                    return right(goid);
                } catch (InvalidResourceException e) {
                    return left(e);
                }
            }
        }, false));

        return Collections.singletonMap( IDENTITY_SELECTOR, goid.toString() );
    }

    // save the entity to the manager
    protected Goid doSaveEntity(E entity) throws SaveException {
        return manager.save(entity);
    }

    // save the entity to the manager
    protected void doSaveEntity(Goid id, E entity) throws SaveException {
        manager.save(id, entity);
    }

    @Override
    public R getResource( final Map<String, String> selectorMap ) throws ResourceNotFoundException {
        return Eithers.extract( transactional( new TransactionalCallback<Either<ResourceNotFoundException,R>>(){
            @Override
            public Either<ResourceNotFoundException,R> execute() throws ObjectModelException {
                try {
                    EntityBag<E> entityBag = selectEntityBag(selectorMap);
                    checkPermitted( OperationType.READ, null, entityBag.getEntity() );
                    return right( identify( asResource( entityBag ), entityBag.getEntity() ) );
                } catch ( ResourceNotFoundException e ) {
                    return left( e );
                }
            }
        }, true ) );
    }

    @Override
    public Collection<Map<String, String>> getResources() {
        Collection<Map<String,String>> resources = Collections.emptyList();

        try {
            List<EH> headers = new ArrayList<>(manager.findAllHeaders());
            headers = accessFilter(headers, manager.getEntityType(), OperationType.READ, null);
            headers = filterHeaders( headers );

            resources = new ArrayList<>( headers.size() );

            for ( EntityHeader header : headers ) {
                resources.add( Collections.singletonMap( IDENTITY_SELECTOR, header.getStrId() ) );
            }
        } catch (FindException e) {
            handleObjectModelException(e);
        }

        return resources;
    }

    @Override
    public List<R> getResources(Integer offset, Integer count, String sort, Boolean ascending, Map<String, List<Object>> filters) {
        try {
            List<E> entities = manager.findPagedMatching(offset, count, sort, ascending, filters);
            entities = accessFilter(entities, manager.getEntityType(), OperationType.READ, null);
            entities = filterEntities(entities);

            return Functions.map(entities, new Functions.Unary<R, E>() {
                @Override
                public R call(E e) {
                    return identify(asResource(e), e);
                }
            });
        } catch (FindException e) {
            handleObjectModelException(e);
        }

        return Collections.emptyList();
    }

    @Override
    public R putResource( final Map<String, String> selectorMap, final Object resource ) throws ResourceNotFoundException, InvalidResourceException {
        checkReadOnly();

        final String id = Eithers.extract2( transactional( new TransactionalCallback<E2<ResourceNotFoundException, InvalidResourceException, String>>() {
            @SuppressWarnings({ "unchecked" })
            @Override
            public E2<ResourceNotFoundException, InvalidResourceException, String> execute() throws ObjectModelException {
                try {
                    final EntityBag<E> oldEntityBag = selectEntityBag( selectorMap );
                    checkPermitted( OperationType.UPDATE, null, oldEntityBag.getEntity() );
                    final EntityBag<E> newEntityBag = fromResourceAsBag( resource );

                    if ( resource instanceof ManagedObject ) {
                        final ManagedObject managedResource = (ManagedObject) resource;
                        setIdentifier( newEntityBag.getEntity(), managedResource.getId(), false );
                        setVersion( newEntityBag.getEntity(), managedResource.getVersion() );
                    }

                    updateEntityBag( oldEntityBag, newEntityBag );

                    verifyIdentifier( oldEntityBag.getEntity().getGoid(),
                                      newEntityBag.getEntity().getGoid() );

                    verifyVersion( oldEntityBag.getEntity().getVersion(),
                                   newEntityBag.getEntity().getVersion() );

                    checkPermitted( OperationType.UPDATE, null, newEntityBag.getEntity() );

                    beforeUpdateEntity( oldEntityBag );

                    for ( PersistentEntity entity : oldEntityBag ) {
                        validate( entity );
                    }

                    if ( manager instanceof RoleAwareEntityManager) {
                        ((RoleAwareEntityManager<E>)manager).updateRoles( oldEntityBag.getEntity() );
                    }
                    manager.update( oldEntityBag.getEntity() );
                    afterUpdateEntity( oldEntityBag );

                    return Eithers.right2( oldEntityBag.getEntity().getId() );
                } catch ( ResourceNotFoundException e ) {
                    return Eithers.left2_1( e );
                } catch ( InvalidResourceException e ) {
                    return Eithers.left2_2( e );
                }
            }
        }, false ) );

        try{
            return getResource( Collections.singletonMap( IDENTITY_SELECTOR, id )); // re-select to get updated version#
        }catch ( PermissionDeniedException e1){
            // return nothing if have no read permission
            return null;
        }
    }

    @Override
    public String deleteResource( final Map<String, String> selectorMap ) throws ResourceNotFoundException {
        checkReadOnly();

        return Eithers.extract( transactional( new TransactionalCallback<Either<ResourceNotFoundException, String>>() {
            @SuppressWarnings({ "unchecked" })
            @Override
            public Either<ResourceNotFoundException, String> execute() throws ObjectModelException {
                try {
                    final EntityBag<E> entityBag = selectEntityBag( selectorMap );

                    checkPermitted( OperationType.DELETE, null, entityBag.getEntity() );

                    beforeDeleteEntity( entityBag );
                    manager.delete( entityBag.getEntity() );
                    afterDeleteEntity( entityBag );

                    return right( entityBag.getEntity().getId() );
                } catch ( ResourceNotFoundException e ) {
                    return left( e );
                }
            }
        }, false ) );
    }

    //- PROTECTED

    protected static final int VERSION_NOT_PRESENT = Integer.MIN_VALUE;

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
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.InvalidResourceException If the resource cannot be converted
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
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.InvalidResourceException If the resource cannot be converted
     */
    protected EntityBag<E> fromResourceAsBag( Object resource ) throws InvalidResourceException {
       return new EntityBag<E>( fromResource( resource ) );
    }

    /**
     * Set identity information on the given resource if it is a ManagedObject.
     *
     * @param resource The resource to process
     * @param entity The entity whose identity should be used
     * @return The (possibly updated) resource
     */
    protected final R identify( final R resource, final E entity ) {
        return identify( resource, entity.getId(), entity.getVersion() );
    }

    /**
     * Set identity information on the given resource if it is a ManagedObject.
     *
     * @param resource The resource to process
     * @param identifier The identifier to use
     * @param version The version to use
     * @return The (possibly updated) resource
     */
    protected final R identify( final R resource,
                                final String identifier,
                                final int version ) {
        if ( resource instanceof ManagedObject ) {
            ManagedObject managedResource = (ManagedObject) resource;
            managedResource.setId( identifier );
            managedResource.setVersion( version );
        }
        return resource;
    }

    /**
     * Override to filter access to the entity headers.
     *
     * @param headers The headers to filter.
     * @return The filtered collection.
     */
    protected List<EH> filterHeaders( final List<EH> headers ) {
        return headers;
    }

    /**
     * Filter access to the entities.
     *
     * @param entities The entities to filter.
     * @return The filtered collection.
     */
    private List<E> filterEntities(final List<E> entities) {
        return Functions.grep(entities, new Functions.Unary<Boolean, E>() {
            @Override
            public Boolean call(E e) {
                return filterEntity(e) != null;
            }
        });
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
     * Extract a selector from the given map.
     *
     * @param selectorMap The selector map.
     * @param selector The selector key.
     * @return The selector value
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.InvalidResourceSelectors If the selector is not present or is null.
     */
    @NotNull
    protected final String getRequiredSelector( @NotNull final Map<String, String> selectorMap,
                                                @NotNull final String selector ) throws InvalidResourceSelectors {
        final Option<String> selectorValue = optional( selectorMap.get( selector ) );
        if ( !selectorValue.isSome() ) {
            throw new InvalidResourceSelectors();
        }
        return selectorValue.some();
    }

    /**
     * Select an entity using the provided selector map.
     *
     * <p>The combination of all selectors must match the entity.</p>
     *
     * @param selectorMap The selectors to use.
     * @return The entity (never null)
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.ResourceNotFoundException If the selectors do not identify an entity.
     */
    @NotNull
    protected final E selectEntity( final Map<String, String> selectorMap ) throws ResourceNotFoundException {
        E entity = null;
        final String id = selectorMap.get( IDENTITY_SELECTOR );
        final String name = selectorMap.get( NAME_SELECTOR );
        final String version = selectorMap.get( VERSION_SELECTOR );
        final String guid = selectorMap.get( GUID_SELECTOR );

        if ( id == null && name == null && guid == null ) {
            throw new InvalidResourceSelectors();
        }

        if ( id != null ) {
            try {
                entity = manager.findByPrimaryKey( toInternalId(id) );
            } catch (FindException e) {
                handleObjectModelException(e);
            }
        }

        if ( entity == null && allowNameSelection && name != null ) {
            try {
                entity = manager.findByUniqueName( name );
            } catch (FindException e) {
                handleObjectModelException(e);
            }
        }

        if ( entity == null && guid != null && manager instanceof GuidBasedEntityManager) {
            @SuppressWarnings("unchecked")
            GuidBasedEntityManager<E> guidManager = (GuidBasedEntityManager<E>) manager;
            try {
                entity = guidManager.findByGuid(guid);
            } catch (FindException e) {
                handleObjectModelException(e);
            }
        }

        if ( entity == null ) {
            entity = selectEntityCustom(selectorMap);
        }

        // Verify all selectors match (selectors must be AND'd)
        if ( entity != null ) {
            if ( id != null && !id.equalsIgnoreCase(entity.getId())) {
                entity = null;
            } else if ( name != null && entity instanceof NamedEntity && !name.equalsIgnoreCase(((NamedEntity)entity).getName())) {
                entity = null;
            } else if (version != null && !version.equals(Integer.toString(entity.getVersion())) ) {
                entity = null;
            } else if (guid != null) {
                try {
                    //TODO replace reflection when entity level interface for GUID support is added
                    final Method getGuidMethod = entity.getClass().getDeclaredMethod("getGuid");
                    final Object guidObj = getGuidMethod.invoke(entity);
                    if ( !guid.equals( guidObj.toString()) ) {
                        entity = null;
                    }
                } catch (ReflectiveOperationException e) {
                    throw new ResourceAccessException(ExceptionUtils.getMessage(e), e);
                }
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
     * If the entity type has any custom selectors then override this method.
     *
     * See  {@link #selectEntityCustom(java.util.Map)}
     * @return set of any custom selectors.
     */
    protected Set<String> getCustomSelectors() {
        return Collections.emptySet();
    }

    /**
     * If the default single selectors are not sufficient to find a unique entity for the entity type, then allow the
     * resource factory impl to look up the entity using it's own custom constraints.
     *
     * If a non null entity is returned, then the returned entity will still need to satisfy the selector constraints
     * imposed by this class - the id, name and guid must match if provided and if they exist for the entity.
     *
     * @param selectorMap map of selectors
     * @return E the found entity or null if no matching entity could be found.
     */
    @Nullable
    protected E selectEntityCustom(final Map<String, String> selectorMap) throws ResourceAccessException, InvalidResourceSelectors {
        return null;
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
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.ResourceNotFoundException If the selectors do not identify an entity.
     * @throws com.l7tech.objectmodel.ObjectModelException If a persistence error occurs
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
     * @throws com.l7tech.objectmodel.ObjectModelException If a persistence error occurs
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
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.InvalidResourceException If the updated entity is not valid
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
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.InvalidResourceException If the updated entity is not valid
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
     * @throws com.l7tech.objectmodel.ObjectModelException If an error occurs
     */
    protected void beforeCreateEntity( final EntityBag<E> entityBag ) throws ObjectModelException {}

    /**
     * Entity creation callback after the primary entity is persisted.
     *
     * <p>This implementation does nothing.</p>
     *
     * @param entityBag The entityBag.
     * @param identifier The identifier for the new entity.
     * @throws com.l7tech.objectmodel.ObjectModelException If an error occurs
     */
    protected void afterCreateEntity( final EntityBag<E> entityBag, Goid identifier ) throws ObjectModelException {}

    /**
     * Entity update callback before the primary entity is persisted.
     *
     * <p>This implementation does nothing.</p>
     *
     * @param entityBag The entityBag.
     * @throws com.l7tech.objectmodel.ObjectModelException If an error occurs
     */
    @SuppressWarnings({ "UnusedDeclaration" })
    protected void beforeUpdateEntity( final EntityBag<E> entityBag ) throws ObjectModelException {}

    /**
     * Entity update callback after the primary entity is persisted.
     *
     * <p>This implementation does nothing.</p>
     *
     * @param entityBag The entityBag.
     * @throws com.l7tech.objectmodel.ObjectModelException If an error occurs
     */
    protected void afterUpdateEntity( final EntityBag<E> entityBag ) throws ObjectModelException {}

    /**
     * Entity deletion callback before the primary entity is persisted.
     *
     * <p>This implementation does nothing.</p>
     *
     * @param entityBag The entityBag.
     * @throws com.l7tech.objectmodel.ObjectModelException If an error occurs
     */
    @SuppressWarnings({ "UnusedDeclaration" })
    protected void beforeDeleteEntity( final EntityBag<E> entityBag ) throws ObjectModelException {}

    /**
     * Entity deletion callback after the primary entity is persisted.
     *
     * <p>This implementation does nothing.</p>
     *
     * @param entityBag The entityBag.
     * @throws com.l7tech.objectmodel.ObjectModelException If an error occurs
     */
    protected void afterDeleteEntity( final EntityBag<E> entityBag ) throws ObjectModelException {}

    /**
     * Convert the given identifier to the internal <code>long</code> format.
     *
     * @param identifier The identifier to process.
     * @return The identifier as a long
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.InvalidResourceSelectors If the given identifier is not valid
     */
    protected final Goid toInternalId( final String identifier ) throws InvalidResourceSelectors {
        if ( identifier == null ) throw new InvalidResourceSelectors();
        try {
            return new Goid(identifier);
        } catch ( IllegalArgumentException nfe ) {
            throw new InvalidResourceSelectors();
        }
    }

    /**
     * Convert the given identifier to the internal <code>goid</code> format.
     *
     * @param identifier The identifier to process.
     * @param identifierDescription A user facing description of the identifier.
     * @return The identifier as a Goid
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.InvalidResourceException If the given identifier is not valid
     */
    protected final Goid toInternalId( final EntityType entityType,
                                       final String identifier,
                                       final String identifierDescription ) throws InvalidResourceException {
        if ( identifier == null )
            throw new InvalidResourceException(
                    InvalidResourceException.ExceptionType.MISSING_VALUES,
                    "Missing " + identifierDescription );
        try {
            return GoidUpgradeMapper.mapId(entityType, identifier);
        } catch ( IllegalArgumentException nfe ) {
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
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.InvalidResourceException If the given identifier is not valid
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
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.InvalidResourceException If the given identifier is not valid
     */
    protected final void setIdentifier( final PersistentEntity entity,
                                        final String identifier,
                                        final boolean required ) throws InvalidResourceException {
        if ( identifier!=null || required ) {
            try {
                entity.setGoid( toInternalId( identifier ) );
            } catch (InvalidResourceSelectors invalidResourceSelectors) {
                throw new InvalidResourceException(InvalidResourceException.ExceptionType.INVALID_VALUES, "invalid identifier");
            }
        }
    }

    /**
     * Verify that the incoming identifier matches the expected value (if present)
     *
     * @param currentId The current identifier for the resource
     * @param updateId The incoming identifier for the resource (may be PersistentEntity.DEFAULT_OID)
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.InvalidResourceException If the identifier is present and does not match
     * @see com.l7tech.objectmodel.PersistentEntity#DEFAULT_GOID
     */
    protected final void verifyIdentifier( final Goid currentId,
                                           final Goid updateId ) throws InvalidResourceException {
        if ( !PersistentEntity.DEFAULT_GOID.equals(currentId) &&
             !PersistentEntity.DEFAULT_GOID.equals(updateId) &&
             !currentId.equals(updateId) ) {
            throw new InvalidResourceException( InvalidResourceException.ExceptionType.INVALID_VALUES, "identifier mismatch" );
        }
    }

    /**
     * Set the version for the entity from the given resource version (if present)
     *
     * @param entity The target entity
     * @param version The resource identifier (may be null)
     * @see #VERSION_NOT_PRESENT
     */
    protected final void setVersion( final PersistentEntity entity,
                                     final Integer version ) {
        entity.setVersion( optional(version).orSome(VERSION_NOT_PRESENT) );
    }

    /**
     * Verify that the incoming version matches the expected value (if present)
     *
     * @param currentVersion The current version for the resource
     * @param updateVersion The incoming version for the resource (may be VERSION_NOT_PRESENT)
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.InvalidResourceException If the version is present and does not match
     * @see #VERSION_NOT_PRESENT
     */
    protected final void verifyVersion( final int currentVersion,
                                        final int updateVersion ) throws InvalidResourceException {
        if ( updateVersion != VERSION_NOT_PRESENT &&
             currentVersion != updateVersion ) {
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.INVALID_VALUES, "invalid version");
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
    static final String GUID_SELECTOR = "guid";
    static final String NAME_SELECTOR = "name";
    static final String VERSION_SELECTOR = "version";

    EntityManagerResourceFactory(final boolean readOnly,
                                 final boolean allowNameSelection,
                                 final RbacServices rbacServices,
                                 final SecurityFilter securityFilter,
                                 final PlatformTransactionManager transactionManager,
                                 final EntityManager<E, EH> manager) {
        this(readOnly, allowNameSelection, false, rbacServices, securityFilter, transactionManager, manager);
    }

    EntityManagerResourceFactory(final boolean readOnly,
                                 final boolean allowNameSelection,
                                 final boolean allowGuidSelection,
                                 final RbacServices rbacServices,
                                 final SecurityFilter securityFilter,
                                 final PlatformTransactionManager transactionManager,
                                 final EntityManager<E, EH> manager) {
        super( rbacServices, securityFilter, transactionManager );
        this.readOnly = readOnly;
        this.allowNameSelection = allowNameSelection;
        this.allowGuidSelection = allowGuidSelection;
        this.manager = manager;
    }

    final <R> R doWithManager( final Functions.UnaryThrows<R,EntityManager<E,EH>,ObjectModelException> callback ) throws ObjectModelException {
        return callback.call( manager );
    }

    //- PRIVATE

    private final boolean readOnly;
    private final boolean allowNameSelection;
    private final boolean allowGuidSelection;
    private final EntityManager<E,EH> manager;

    private void checkReadOnly() {
        if ( isReadOnly() ) throw new IllegalStateException("Read only");
    }

}
