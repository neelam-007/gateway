package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.StoredPasswordMO;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.security.rbac.SecurityFilter;
import com.l7tech.util.ExceptionUtils;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Resource factory for stored/secure passwords.
 */
@ResourceFactory.ResourceType(type=StoredPasswordMO.class)
public class SecurePasswordResourceFactory extends EntityManagerResourceFactory<StoredPasswordMO, SecurePassword, EntityHeader> {

    //- PUBLIC

    public SecurePasswordResourceFactory( final RbacServices services,
                                          final SecurityFilter securityFilter,
                                          final PlatformTransactionManager transactionManager,
                                          final SecurePasswordManager securePasswordManager ) {
        super( false, true, services, securityFilter, transactionManager, securePasswordManager );
        this.securePasswordManager = securePasswordManager;
    }

    //- PROTECTED

    @Override
    protected StoredPasswordMO asResource( final SecurePassword entity ) {
        final StoredPasswordMO storedPassword = ManagedObjectFactory.createStoredPassword();

        storedPassword.setName( entity.getName() );
        storedPassword.setProperties( getProperties( entity, SecurePassword.class ) );

        return storedPassword;
    }

    @Override
    protected SecurePassword fromResource( final Object resource ) throws InvalidResourceException {
        if ( !(resource instanceof StoredPasswordMO) )
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.UNEXPECTED_TYPE, "expected stored password");

        final StoredPasswordMO storedPassword = (StoredPasswordMO) resource;

        final SecurePassword securePasswordEntity = new SecurePassword();
        securePasswordEntity.setName( asName(storedPassword.getName()) );
        setProperties( securePasswordEntity, storedPassword.getProperties(), SecurePassword.class );

        // After setProperties as this will set the last updated time
        if ( storedPassword.getPassword() != null ) {
            encodeAndSetPassword( securePasswordEntity, storedPassword.getPassword().toCharArray() );
        }

        return securePasswordEntity;
    }

    @Override
    protected void updateEntity( final SecurePassword oldEntity,
                                 final SecurePassword newEntity ) throws InvalidResourceException {
        oldEntity.setName( newEntity.getName() );
        oldEntity.setDescription( newEntity.getDescription() );
        oldEntity.setUsageFromVariable( newEntity.isUsageFromVariable() );

        if ( newEntity.getEncodedPassword() != null ) {
            oldEntity.setEncodedPassword( newEntity.getEncodedPassword() );
            oldEntity.setLastUpdate( newEntity.getLastUpdate() );
        }
    }

    //- PRIVATE

    private final SecurePasswordManager securePasswordManager;

    /**
     * Set the password and updated time.
     */
    private void encodeAndSetPassword( final SecurePassword securePasswordEntity, final char[] passwordPassword ) {
        try {
            securePasswordEntity.setEncodedPassword( securePasswordManager.encryptPassword( passwordPassword ) );
            securePasswordEntity.setLastUpdate( System.currentTimeMillis() );
        } catch ( FindException e ) {
            throw new ResourceAccessException( "Unable to process password '"+ ExceptionUtils.getMessage( e ) +"'", e );
        }
    }
}
