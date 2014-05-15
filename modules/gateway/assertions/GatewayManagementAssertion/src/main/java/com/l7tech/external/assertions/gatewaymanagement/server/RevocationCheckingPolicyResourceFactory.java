package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.RevocationCheckingPolicyMO;
import com.l7tech.gateway.common.security.RevocationCheckPolicy;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.server.identity.cert.RevocationCheckPolicyManager;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.security.rbac.SecurityFilter;
import com.l7tech.server.security.rbac.SecurityZoneManager;
import org.springframework.transaction.PlatformTransactionManager;

/**
 *
 */
@ResourceFactory.ResourceType(type=RevocationCheckingPolicyMO.class)
public class RevocationCheckingPolicyResourceFactory extends SecurityZoneableEntityManagerResourceFactory<RevocationCheckingPolicyMO, RevocationCheckPolicy, EntityHeader> {

    //- PUBLIC

    public RevocationCheckingPolicyResourceFactory( final RbacServices services,
                                                    final SecurityFilter securityFilter,
                                                    final PlatformTransactionManager transactionManager,
                                                    final RevocationCheckPolicyManager revocationCheckPolicyManager,
                                                    final SecurityZoneManager securityZoneManager ) {
        super( true, true, services, securityFilter, transactionManager, revocationCheckPolicyManager, securityZoneManager );
    }

    //- PROTECTED

    @Override
    public RevocationCheckingPolicyMO asResource( final RevocationCheckPolicy entity ) {
        final RevocationCheckingPolicyMO revocationCheckingPolicy = ManagedObjectFactory.createRevocationCheckingPolicy();

        revocationCheckingPolicy.setName( entity.getName() );

        // handle SecurityZone
        doSecurityZoneAsResource( revocationCheckingPolicy, entity );

        return revocationCheckingPolicy;
    }

    @Override
    public RevocationCheckPolicy fromResource(Object resource, boolean strict) throws InvalidResourceException {
        if (!(resource instanceof RevocationCheckingPolicyMO)) {
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.UNEXPECTED_TYPE, "expected RevocationCheckingPolicy");
        }
        final RevocationCheckingPolicyMO revocationCheckingPolicyMO = (RevocationCheckingPolicyMO) resource;
        final RevocationCheckPolicy revocationCheckPolicy = new RevocationCheckPolicy();

        revocationCheckPolicy.setName(revocationCheckingPolicyMO.getName());

        return revocationCheckPolicy;
    }
}
