package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.gateway.api.AssertionSecurityZoneMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.policy.AssertionAccess;
import com.l7tech.server.policy.AssertionAccessManager;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.security.rbac.SecurityFilter;
import com.l7tech.server.security.rbac.SecurityZoneManager;
import org.springframework.transaction.PlatformTransactionManager;

/**
 *
 */
@ResourceFactory.ResourceType(type=AssertionSecurityZoneMO.class)
public class AssertionSecurityZoneResourceFactory extends SecurityZoneableEntityManagerResourceFactory<AssertionSecurityZoneMO, AssertionAccess, EntityHeader> {

    //- PUBLIC

    public AssertionSecurityZoneResourceFactory(final RbacServices rbacServices,
                                                final SecurityFilter securityFilter,
                                                final PlatformTransactionManager transactionManager,
                                                final AssertionAccessManager assertionAccessManager,
                                                final SecurityZoneManager securityZoneManager) {
        super(false, true, rbacServices, securityFilter, transactionManager, assertionAccessManager, securityZoneManager);
        this.assertionAccessManager = assertionAccessManager;
    }

    //- PROTECTED

    @Override
    protected AssertionSecurityZoneMO asResource(AssertionAccess entity) {
        AssertionSecurityZoneMO resource = ManagedObjectFactory.createAssertionAccess();

        resource.setName( entity.getName() );

        // handle SecurityZone
        doSecurityZoneAsResource( resource, entity );

        return resource;
    }

    @Override
    protected AssertionAccess fromResource(Object resource) throws InvalidResourceException {

        if ( !(resource instanceof AssertionSecurityZoneMO) )
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.UNEXPECTED_TYPE, "expected assertion access");

        final AssertionSecurityZoneMO assertionResource = (AssertionSecurityZoneMO) resource;

        final AssertionAccess accessEntity;
        accessEntity = new AssertionAccess();
        accessEntity.setName( assertionResource.getName() );

        // handle SecurityZone
        doSecurityZoneFromResource(assertionResource, accessEntity);

        return accessEntity;
    }

    @Override
    protected void updateEntity(AssertionAccess oldEntity, AssertionAccess newEntity) throws InvalidResourceException {

        oldEntity.setName( newEntity.getName() );
        oldEntity.setSecurityZone( newEntity.getSecurityZone() );
    }

    //- PRIVATE

    private AssertionAccessManager assertionAccessManager;
}
