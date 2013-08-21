package com.l7tech.external.assertions.gatewaymanagement.server;

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
    InterfaceTagMO internalAsResource( @NotNull final InterfaceTag interfaceTag ) {
        final InterfaceTagMO resource = ManagedObjectFactory.createInterfaceTag();
        resource.setName( interfaceTag.getName() );
        resource.setAddressPatterns( new ArrayList<String>(interfaceTag.getIpPatterns()) );
        return resource;
    }

    @NotNull
    @Override
    Pair<InterfaceTag,Integer> internalFromResource( @NotNull final Object resource ) throws InvalidResourceException {
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
                         @NotNull final InterfaceTag newInternal ) {
        oldInternal.setIpPatterns( newInternal.getIpPatterns() );
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
