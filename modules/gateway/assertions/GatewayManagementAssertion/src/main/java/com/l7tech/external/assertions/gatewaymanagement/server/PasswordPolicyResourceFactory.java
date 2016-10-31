package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.PasswordPolicyMO;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.identity.IdentityProviderPasswordPolicy;
import com.l7tech.identity.IdentityProviderPasswordPolicyManager;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.security.rbac.SecurityFilter;
import com.l7tech.server.security.rbac.SecurityZoneManager;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.IOException;

/**
 * Resource factory for password rules.
 */
@ResourceFactory.ResourceType(type=PasswordPolicyMO.class)
public class PasswordPolicyResourceFactory extends SecurityZoneableEntityManagerResourceFactory<PasswordPolicyMO, IdentityProviderPasswordPolicy, EntityHeader> {

    //- PUBLIC

    private final IdentityProviderPasswordPolicyManager passwordPolicyManger;

    //- PROTECTED

    public PasswordPolicyResourceFactory( final RbacServices services,
                                          final SecurityFilter securityFilter,
                                          final PlatformTransactionManager transactionManager,
                                          final IdentityProviderPasswordPolicyManager passwordPolicyManager,
                                          final SecurityZoneManager securityZoneManager ) {
        super( false, true, services, securityFilter, transactionManager, passwordPolicyManager, securityZoneManager );
        this.passwordPolicyManger = passwordPolicyManager;
    }

    @Override
    public PasswordPolicyMO asResource( final IdentityProviderPasswordPolicy entity ) {
        final PasswordPolicyMO passwordPolicy = ManagedObjectFactory.createPasswordPolicy();

        passwordPolicy.setProperties( getProperties( entity, IdentityProviderPasswordPolicy.class ) );

        // handle SecurityZone
        doSecurityZoneAsResource( passwordPolicy, entity );

        return passwordPolicy;
    }

    @Override
    public IdentityProviderPasswordPolicy fromResource( final Object resource, boolean strict ) throws InvalidResourceException {
        if ( !(resource instanceof PasswordPolicyMO) )
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.UNEXPECTED_TYPE, "expected stored password");

        final PasswordPolicyMO passwordPolicy = (PasswordPolicyMO) resource;

        final IdentityProviderPasswordPolicy passwordPolicyEntity = new IdentityProviderPasswordPolicy();
        setProperties( passwordPolicyEntity, passwordPolicy.getProperties(), IdentityProviderPasswordPolicy.class );

        // handle SecurityZone
        doSecurityZoneFromResource( passwordPolicy, passwordPolicyEntity, strict );

        return passwordPolicyEntity;
    }

    //- PRIVATE

    @Override
    protected void updateEntity( final IdentityProviderPasswordPolicy oldEntity,
                                 final IdentityProviderPasswordPolicy newEntity ) throws InvalidResourceException {
        try {
            oldEntity.setSerializedProps(newEntity.getSerializedProps());
        } catch (IOException e){

        }
    }
}
