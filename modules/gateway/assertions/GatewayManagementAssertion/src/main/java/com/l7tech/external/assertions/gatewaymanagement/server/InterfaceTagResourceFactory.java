package com.l7tech.external.assertions.gatewaymanagement.server;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.l7tech.gateway.api.InterfaceTagMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.transport.InterfaceTag;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.security.rbac.SecurityFilter;
import com.l7tech.util.Charsets;
import com.l7tech.util.Functions;
import com.l7tech.util.Option;
import com.l7tech.util.Pair;
import com.sun.istack.Nullable;
import org.jetbrains.annotations.NotNull;
import org.springframework.transaction.PlatformTransactionManager;

import java.text.ParseException;
import java.util.*;

import static com.l7tech.util.CollectionUtils.foreach;
import static com.l7tech.util.Option.optional;
import static com.l7tech.util.Pair.pair;

/**
 * Resource factory for interface tags.
 */
@ResourceFactory.ResourceType(type=InterfaceTagMO.class)
public class InterfaceTagResourceFactory extends ClusterPropertyBackedResourceFactory<InterfaceTagMO,InterfaceTag> {

    //- PACKAGE

    InterfaceTagResourceFactory( final RbacServices rbacServices,
                                 final SecurityFilter securityFilter,
                                 final PlatformTransactionManager transactionManager,
                                 final EntityManager<ClusterProperty, EntityHeader> clusterPropertyEntityHeaderEntityManager ) {
        super( false, rbacServices, securityFilter, transactionManager, clusterPropertyEntityHeaderEntityManager, "interfaceTags" );
    }

    @NotNull
    @Override
    Collection<InterfaceTag> parseProperty( @NotNull final String value ) {
        try {
            return InterfaceTag.parseMultiple( value );
        } catch (ParseException e) {
            throw new ResourceAccessException( e );
        }
    }

    @NotNull
    @Override
    String formatProperty( @NotNull final Collection<InterfaceTag> internalValues ) {
        return InterfaceTag.toString( new LinkedHashSet<InterfaceTag>(internalValues) );
    }

    @NotNull
    @Override
    String getIdentifier( @NotNull final InterfaceTag internalValue ) {
        return nameAsIdentifier( internalValue.getName() );
    }

    @NotNull
    @Override
    Option<InterfaceTag> selectInternal( @NotNull final String identifier,
                                         @NotNull final Collection<InterfaceTag> internalValues) {
        return optional(Functions.grepFirst(internalValues, new Functions.Unary<Boolean, InterfaceTag>() {
            @Override
            public Boolean call(final InterfaceTag interfaceTag) {
                return identifier.equals(getIdentifier( interfaceTag ));
            }
        }));
    }

    @NotNull
    @Override
    Option<InterfaceTag> selectInternalByName( @NotNull final String name,
                                               @NotNull final Collection<InterfaceTag> internalValues ) {
        return selectInternal( nameAsIdentifier( name ), internalValues );
    }

    @NotNull
    @Override
    public InterfaceTagMO internalAsResource( @NotNull final InterfaceTag interfaceTag ) {
        final InterfaceTagMO resource = ManagedObjectFactory.createInterfaceTag();
        resource.setId( getIdentifier(interfaceTag));
        resource.setName( interfaceTag.getName() );
        resource.setAddressPatterns( new ArrayList<String>(interfaceTag.getIpPatterns()) );
        return resource;
    }

    @NotNull
    @Override
    public Pair<InterfaceTag,Integer> internalFromResource( @NotNull final Object resource ) throws InvalidResourceException {
        if ( !(resource instanceof InterfaceTagMO) )
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.UNEXPECTED_TYPE, "expected interface tag");

        final InterfaceTagMO interfaceTagMO = (InterfaceTagMO) resource;

        final String name = interfaceTagMO.getName();
        final Set<String> ipPatterns = new LinkedHashSet<String>(interfaceTagMO.getAddressPatterns());

        if ( !InterfaceTag.isValidName(name) ) {
            throw new InvalidResourceException( InvalidResourceException.ExceptionType.INVALID_VALUES,  "Invalid identifier: " + name );
        }

        foreach(ipPatterns, true, new Functions.UnaryVoidThrows<String, InvalidResourceException>() {
            @Override
            public void call(final String ipPattern) throws InvalidResourceException {
                if (!InterfaceTag.isValidPattern(ipPattern)) {
                    throw new InvalidResourceException(InvalidResourceException.ExceptionType.INVALID_VALUES, "Invalid address pattern: " + ipPattern);
                }
            }
        });

        return pair(new InterfaceTag( name, ipPatterns ), interfaceTagMO.getVersion());
    }

    @Override
    void updateInternal( @NotNull final InterfaceTag oldInternal,
                         @NotNull final InterfaceTag newInternal ) throws InvalidResourceException {
        if(!oldInternal.getName().equals(newInternal.getName())){
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.INVALID_VALUES, "Cannot change an interface tag's name");
        }
        oldInternal.setIpPatterns( newInternal.getIpPatterns() );
    }

    @Override
    protected List<InterfaceTag> filterAndSortEntities(List<InterfaceTag> entities, final String sortKey, final Boolean ascending,final Map<String, List<Object>> filtersMap) {

        entities = Lists.newArrayList(Iterables.filter(entities, new Predicate<InterfaceTag>() {
            @Override
            public boolean apply(@Nullable InterfaceTag interfaceTag) {
                boolean match = true;
                if (filtersMap.containsKey("name")) {
                    match = filtersMap.get("name").contains(interfaceTag.getName());
                }
                if (match && filtersMap.containsKey("id")) {
                    match = match && filtersMap.get("id").contains(nameAsIdentifier(interfaceTag.getName()));
                }
                return match;
            }
        }));

        Collections.sort(entities,new Comparator<InterfaceTag>() {
            @Override
            public int compare(InterfaceTag o1, InterfaceTag o2) {
                if(sortKey == null)
                    return 0;

                if(sortKey.equals("name")){
                    return (ascending == null || ascending) ? o1.getName().compareTo(o2.getName()) :  o2.getName().compareTo(o1.getName());
                }
                if(sortKey.equals("id")){
                    return (ascending == null || ascending) ? nameAsIdentifier(o1.getName()).compareTo(nameAsIdentifier(o2.getName())) :
                                       nameAsIdentifier(o2.getName()).compareTo(nameAsIdentifier(o1.getName()));
                }
                return 0;
            }
        });
        return entities;
    }

    //- PRIVATE

    /**
     * We generate an identifier from the unique name, this should allow
     * for interface tags to be stored as separate entities in the future.
     */
    private String nameAsIdentifier( final String name ) {
        return UUID.nameUUIDFromBytes( name.getBytes( Charsets.UTF8 ) ).toString();
    }

}
