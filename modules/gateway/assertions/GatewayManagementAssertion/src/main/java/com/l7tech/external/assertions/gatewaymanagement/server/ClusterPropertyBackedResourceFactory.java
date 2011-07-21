package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.security.rbac.SecurityFilter;
import com.l7tech.util.Functions.Unary;
import com.l7tech.util.Functions.UnaryThrows;
import static com.l7tech.util.Functions.grep;
import static com.l7tech.util.Functions.map;
import static com.l7tech.util.Functions.partial;
import com.l7tech.util.Option;
import static com.l7tech.util.Option.optional;
import org.jetbrains.annotations.NotNull;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.*;

/**
 * Abstract resource factory for resources stored as a cluster property.
 *
 * @param <R> The "user visible" resource type
 * @param <RI> The internal representation for the resource type
 */
abstract class ClusterPropertyBackedResourceFactory<R,RI> extends EntityManagerResourceFactory<R, ClusterProperty, EntityHeader> {

    //- PUBLIC

    @Override
    public final Map<String, String> createResource( final Object resource ) throws InvalidResourceException {
        final String id = transactional( new TransactionalCallback<String,InvalidResourceException>(){
            @Override
            public String execute() throws ObjectModelException, InvalidResourceException {
                final RI internalValue = internalFromResource( resource );
                final ClusterProperty property = getClusterPropertyForUpdate();
                final Collection<RI> internalValues = parseProperty( property );
                final Collection<RI> updatedInternalValues = createInternal( internalValue, internalValues );
                saveOrUpdateClusterProperty(property, formatProperty(updatedInternalValues));

                return getIdentifier(internalValue);
            }
        }, false, InvalidResourceException.class );

        return Collections.singletonMap(IDENTITY_SELECTOR, id);
    }

    @Override
    public final Collection<Map<String, String>> getResources() {
        return transactional(new TransactionalCallback<Collection<Map<String, String>>, ResourceAccessException>() {
            @Override
            public Collection<Map<String, String>> execute() throws ObjectModelException {
                try {
                    final ClusterProperty property = getClusterPropertyForRead();
                    final Collection<RI> internalValues = parseProperty( property );
                    return map( internalValues, new Unary<Map<String, String>, RI>() {
                        @Override
                        public Map<String, String> call( final RI ri ) {
                            return Collections.singletonMap( IDENTITY_SELECTOR, getIdentifier( ri ) );
                        }
                    } );
                } catch (ResourceNotFoundException e) {
                    return Collections.emptyList();
                }
            }
        }, true);
    }

    @Override
    public final R getResource( final Map<String, String> selectorMap ) throws ResourceNotFoundException {
        return transactional(new TransactionalCallback<R, ResourceNotFoundException>() {
            @Override
            public R execute() throws ObjectModelException, ResourceNotFoundException {
                final ClusterProperty property = getClusterPropertyForRead( optional( selectorMap.get( VERSION_SELECTOR ) ));
                final Option<R> resource = asResource(selectorMap, property);
                if (!resource.isSome()) throw new InvalidResourceSelectors();
                return resource.some();
            }
        }, true, ResourceNotFoundException.class);
    }

    @Override
    public final R putResource( final Map<String, String> selectorMap,
                                final Object resource ) throws ResourceNotFoundException, InvalidResourceException {
        transactional(new TransactionalCallback<Void, Exception>() {
            @Override
            public Void execute() throws ObjectModelException, InvalidResourceException, ResourceNotFoundException {
                final RI internalValue = internalFromResource(resource);
                final ClusterProperty property = getClusterPropertyForUpdate();
                final Collection<RI> internalValues = parseProperty( property );
                final Collection<RI> updatedInternalValues = putInternal(internalValue, internalValues);
                saveOrUpdateClusterProperty(property, formatProperty(updatedInternalValues));
                return null;
            }
        }, false, ResourceNotFoundException.class, InvalidResourceException.class);

        return getResource( selectorMap ); // re-select to get updated version#
    }

    @Override
    public final String deleteResource( final Map<String, String> selectorMap ) throws ResourceNotFoundException {
        return transactional( new TransactionalCallback<String,ResourceNotFoundException>(){
            @Override
            public String execute() throws ObjectModelException, ResourceNotFoundException {
                final ClusterProperty property = getClusterPropertyForUpdate( false, selectorMap.toString() );
                final Collection<RI> internalValues = parseProperty( property );
                final Option<RI> internalValue = selectInternal( selectorMap, internalValues );
                final Collection<RI> updatedInternalValues = deleteInternal( selectorMap, internalValue, internalValues );
                saveOrUpdateClusterProperty(property, formatProperty(updatedInternalValues));

                return getIdentifier( internalValue.some() );
            }
        }, false, ResourceNotFoundException.class );
    }

    //- PACKAGE

    ClusterPropertyBackedResourceFactory( final boolean readOnly,
                                          final RbacServices rbacServices,
                                          final SecurityFilter securityFilter,
                                          final PlatformTransactionManager transactionManager,
                                          final EntityManager<ClusterProperty, EntityHeader> clusterPropertyEntityHeaderEntityManager,
                                          final String propertyName ) {
        super(readOnly, true, rbacServices, securityFilter, transactionManager, clusterPropertyEntityHeaderEntityManager);
        this.propertyName = propertyName;
    }

    /**
     * Parse the property into it's internal representation.
     *
     * @param property The cluster property
     * @return The internal representation collection
     */
    Collection<RI> parseProperty( final ClusterProperty property ) {
        final Option<String> value = optional( property.getValue() );
        return value.map(new Unary<Collection<RI>, String>() {
            @Override
            public Collection<RI> call( final String propertyValue ) {
                return parseProperty( propertyValue );
            }
        }).orSome( Collections.<RI>emptyList());
    }

    /**
     * Parse the property into it's internal representation.
     *
     * @param value The cluster property value
     * @return The internal representation collection
     */
    @NotNull
    abstract Collection<RI> parseProperty( @NotNull final String value );

    /**
     * Format the internal representation values as a cluster property value.
     *
     * @param internalValues The values to format
     * @return The formatted value
     */
    @NotNull
    abstract String formatProperty( @NotNull final Collection<RI> internalValues );

    /**
     * Get the external selectors for the given internal value.
     *
     * @param internalValue The internal value
     * @return The selectors
     */
    Map<String,String> getSelectors( @NotNull RI internalValue ) {
        return Collections.singletonMap( IDENTITY_SELECTOR, getIdentifier(internalValue) );
    }

    /**
     * Get the external identifier for the given internal value.
     *
     * @param internalValue The internal value
     * @return The identifier
     */
    @NotNull
    abstract String getIdentifier( @NotNull RI internalValue );

    /**
     * Select an internal representation using the given selectors.
     *
     * @param selectorMap The selectors to use
     * @param internalValues The values to select from
     * @return The optional internal representation
     * @throws InvalidResourceSelectors if a selector is used that is not supported
     */
    Option<RI> selectInternal( @NotNull final Map<String,String> selectorMap,
                               @NotNull final Collection<RI> internalValues ) throws InvalidResourceSelectors {
        final Option<String> identifier = optional( selectorMap.get( IDENTITY_SELECTOR ) );
        final Option<String> name = optional( selectorMap.get( NAME_SELECTOR ) );

        final Option<RI> internal = identifier.map( new Unary<RI, String>() {
            @Override
            public RI call( final String identifier ) {
                return selectInternal( identifier, internalValues ).toNull();
            }
        } );

        try {
            return internal.orElse( partial( Option.<RI,String>map(), name, new Unary<RI, String>() {
                @Override
                public RI call( final String name ) {
                    return selectInternalByName( name, internalValues ).toNull();
                }
            } ) );
        } catch ( NameSelectionNotSupported e ) {
            throw new InvalidResourceSelectors();
        }
    }

    /**
     * Select an internal representation from the given collection.
     *
     * @param identifier The identifier for the internal value
     * @param internalValues The values to select from
     * @return The optional internal representation
     */
    @NotNull
    abstract Option<RI> selectInternal( @NotNull final String identifier,
                                        @NotNull final Collection<RI> internalValues );

    /**
     * Select an internal representation from the given collection by name.
     *
     * @param name The name for the internal value
     * @param internalValues The values to select from
     * @return The optional internal representation
     */
    @NotNull
    Option<RI> selectInternalByName( @NotNull final String name,
                                     @NotNull final Collection<RI> internalValues ) {
        throw new NameSelectionNotSupported();
    }

    /**
     * Create a new internal value and add to the given existing values.
     *
     * @param internalValue The value to add
     * @param internalValues The existing values
     * @return The updated value collection
     * @throws DuplicateResourceAccessException If the internal value is a duplicate
     */
    @NotNull
    Collection<RI> createInternal( @NotNull final RI internalValue,
                                   @NotNull final Collection<RI> internalValues ) {
        final Map<String,String> selectorMap = getSelectors( internalValue );
        final Option<RI> internal;
        try {
            internal = selectInternal( selectorMap, internalValues );
        } catch ( InvalidResourceSelectors invalidResourceSelectors ) {
            throw new ResourceAccessException( invalidResourceSelectors );
        }

        if ( internal.isSome() ) {
            throw new DuplicateResourceAccessException("Resource already exists: " + selectorMap);
        }

        final List<RI> updated = new ArrayList<RI>();
        updated.addAll( internalValues );
        updated.add( internalValue );

        return updated;
    }

    /**
     * Put the given resource to the internal value collection.
     *
     * @param internalValue The updated internal value
     * @param internalValues The existing internal values
     * @return The (updated) existing internal values
     * @throws ResourceNotFoundException If the internal value does not currently exist
     */
    Collection<RI> putInternal( final RI internalValue,
                                final Collection<RI> internalValues ) throws ResourceNotFoundException {
        final String identifier = getIdentifier( internalValue );
        final Option<RI> internal = selectInternal( identifier, internalValues );

        if ( !internal.isSome() ) {
            throw new ResourceNotFoundException( "Resource not found: " + identifier );
        }

        updateInternal( internal.some(), internalValue );

        return internalValues;
    }

    /**
     * Update the existing (old) internal representation from the new value
     *
     * @param oldInternal The existing internal representation
     * @param newInternal The updated internal representation.
     */
    abstract void updateInternal( @NotNull final RI oldInternal,
                                  @NotNull final RI newInternal );

    /**
     * Delete the given internal value from the given values.
     *
     * @param selectorMap The selectors for the internal value to remove
     * @param internalValue The internal value to remove
     * @param internalValues The existing internal values
     * @return The updated value collection
     * @throws ResourceNotFoundException If the internal value is not found
     */
    Collection<RI> deleteInternal( final Map<String,String> selectorMap,
                                   final Option<RI> internalValue,
                                   final Collection<RI> internalValues ) throws ResourceNotFoundException {
        if ( !internalValue.isSome() ) {
            throw new ResourceNotFoundException( "Resource not found: " + selectorMap );
        }

        return grep( internalValues, new Unary<Boolean, RI>() {
            @Override
            public Boolean call( final RI ri ) {
                return ri != internalValue.some();
            }
        } );
    }

    /**
     * Convert the internal representation to the resource representation.
     *
     * @param internalValue The value to convert
     * @return The resource
     */
    @NotNull
    abstract R internalAsResource( @NotNull final RI internalValue );

    /**
     * Convert the resource representation to the internal representation.
     *
     * @param resource The value to convert
     * @return The identifier and internal representation
     */
    @NotNull
    abstract RI internalFromResource( @NotNull final Object resource ) throws InvalidResourceException;

    //- PRIVATE

    private final String propertyName;

    /**
     * Parse the cluster property and extract/convert the internal represention
     */
    private Option<R> asResource( final Map<String, String> selectorMap,
                                  final ClusterProperty clusterProperty ) throws ResourceNotFoundException {
        final Collection<RI> internalValues = parseProperty( clusterProperty );
        final Option<RI> internalValue = selectInternal(selectorMap, internalValues);
        return internalValue.map( new Unary<R, RI>() {
            @Override
            public R call(final RI internalValue) {
                return identify( internalAsResource(internalValue), getIdentifier(internalValue), clusterProperty.getVersion() );
            }
        });
    }

    /**
     * Get the cluster property for read access if permitted.
     */
    private ClusterProperty getClusterPropertyForRead() throws ResourceNotFoundException {
        return getClusterPropertyForRead( Option.<String>none() );
    }

    /**
     * Get the cluster property for read access if permitted.
     */
    private ClusterProperty getClusterPropertyForRead( final Option<String> version ) throws ResourceNotFoundException {
        final Map<String,String> selectorMap = new HashMap<String, String>();
        selectorMap.put( NAME_SELECTOR, propertyName );
        if ( version.isSome() ) {
            selectorMap.put( VERSION_SELECTOR, version.some() );
        }

        final ClusterProperty property = selectEntity( selectorMap );
        checkPermitted(OperationType.READ, null, property);
        return property;
    }

    /**
     * Get the cluster property for create/update if permitted.
     */
    private ClusterProperty getClusterPropertyForUpdate() {
        try {
            return getClusterPropertyForUpdate( true, "" );
        } catch (ResourceNotFoundException e) {
            throw new ResourceAccessException( e ); // does not occur
        }
    }

    /**
     * Get the cluster property for create/update if permitted.
     */
    private ClusterProperty getClusterPropertyForUpdate( final boolean create,
                                                         final String description ) throws ResourceNotFoundException {
        ClusterProperty property;
        try {
            property = selectEntity(Collections.singletonMap(NAME_SELECTOR, propertyName));
        } catch ( ResourceNotFoundException e ) {
            if ( create ) {
                property = new  ClusterProperty( propertyName, null );
            } else {
                throw new ResourceNotFoundException( "Resource not found " + description );
            }
        }

        if ( property.getOid() == PersistentEntity.DEFAULT_OID ) {
            checkPermitted( OperationType.CREATE, null, property );
        } else {
            checkPermitted( OperationType.UPDATE, null, property );
        }
        return property;
    }

    /**
     * Persist the cluster property with the given value.
     */
    private void saveOrUpdateClusterProperty( final ClusterProperty property,
                                              final String updatedValue ) throws ObjectModelException {
        property.setValue( updatedValue );

        try {
            validate(property);
        } catch (InvalidResourceException e) {
            throw new ResourceAccessException( "Updated cluster property value is invalid", e);
        }

        final long id = doWithManager(new UnaryThrows<Long, EntityManager<ClusterProperty, EntityHeader>, ObjectModelException>() {
            @Override
            public Long call(final EntityManager<ClusterProperty, EntityHeader> manager) throws ObjectModelException {
                if ( property.getOid() == PersistentEntity.DEFAULT_OID ) {
                    return manager.save(property);
                } else {
                    manager.update(property);
                    return property.getOid();
                }
            }
        });

        EntityContext.setEntityInfo(getType(), Long.toString(id));
    }

    private static final class NameSelectionNotSupported extends RuntimeException {
    }
}
