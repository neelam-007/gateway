package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.gateway.api.IdentityProviderMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.security.rbac.SecurityFilter;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 
 */
@ResourceFactory.ResourceType(type=IdentityProviderMO.class)
public class IdentityProviderResourceFactory extends EntityManagerResourceFactory<IdentityProviderMO, IdentityProviderConfig, EntityHeader>{

    //- PUBLIC

    public IdentityProviderResourceFactory( final RbacServices services,
                                            final SecurityFilter securityFilter,
                                            final PlatformTransactionManager transactionManager,
                                            final IdentityProviderConfigManager identityProviderConfigManager ) {
        super( true, true, services, securityFilter, transactionManager, identityProviderConfigManager );
    }

    //- PROTECTED

    @Override
    protected IdentityProviderMO asResource( final IdentityProviderConfig identityProviderConfig ) {
        IdentityProviderMO identityProvider = ManagedObjectFactory.createIdentityProvider();

        identityProvider.setId( identityProviderConfig.getId() );
        identityProvider.setVersion( identityProviderConfig.getVersion() );
        identityProvider.setName( identityProviderConfig.getName() );
        switch ( identityProviderConfig.getTypeVal() ) {
            case 1:
                identityProvider.setIdentityProviderType( IdentityProviderMO.IdentityProviderType.INTERNAL );
                break;
            case 2:
                identityProvider.setIdentityProviderType( IdentityProviderMO.IdentityProviderType.LDAP );
                break;
            case 3:
                identityProvider.setIdentityProviderType( IdentityProviderMO.IdentityProviderType.FEDERATED );
                break;
            default:
                throw new ResourceAccessException("Unknown identity provider type '"+identityProviderConfig.getTypeVal()+"'.");
        }

        return identityProvider;
    }
}
