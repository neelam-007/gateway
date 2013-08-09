package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.RevocationCheckingPolicyMO;
import com.l7tech.gateway.common.security.RevocationCheckPolicy;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.server.identity.cert.RevocationCheckPolicyManager;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.security.rbac.SecurityFilter;
import org.springframework.transaction.PlatformTransactionManager;

/**
 *
 */
@ResourceFactory.ResourceType(type=RevocationCheckingPolicyMO.class)
public class RevocationCheckingPolicyResourceFactory extends GoidEntityManagerResourceFactory<RevocationCheckingPolicyMO, RevocationCheckPolicy, EntityHeader> {

    //- PUBLIC

    public RevocationCheckingPolicyResourceFactory( final RbacServices services,
                                                    final SecurityFilter securityFilter,
                                                    final PlatformTransactionManager transactionManager,
                                                    final RevocationCheckPolicyManager revocationCheckPolicyManager ) {
        super( true, true, services, securityFilter, transactionManager, revocationCheckPolicyManager );
    }

    //- PROTECTED

    @Override
    protected RevocationCheckingPolicyMO asResource( final RevocationCheckPolicy entity ) {
        final RevocationCheckingPolicyMO revocationCheckingPolicy = ManagedObjectFactory.createRevocationCheckingPolicy();

        revocationCheckingPolicy.setName( entity.getName() );

        return revocationCheckingPolicy;
    }
}
