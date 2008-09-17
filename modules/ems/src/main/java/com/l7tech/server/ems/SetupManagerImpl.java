package com.l7tech.server.ems;

import com.l7tech.identity.internal.InternalUser;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.identity.UserBean;
import com.l7tech.identity.User;

import com.l7tech.server.GatewayLicenseManager;
import com.l7tech.server.audit.AuditContext;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.server.identity.internal.InternalUserManager;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.gateway.common.InvalidLicenseException;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.gateway.common.security.rbac.Permission;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.IdentityHeader;
import com.l7tech.objectmodel.InvalidPasswordException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.util.ResourceUtils;
import static org.springframework.transaction.annotation.Propagation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.InitializingBean;

import javax.security.auth.Subject;
import javax.sql.DataSource;
import java.util.Collections;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.security.AccessController;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLWarning;

/**
 * Encapsulates behavior for initial setup of a new EMS instance.
 */
@Transactional(propagation=REQUIRED, rollbackFor=Throwable.class)
public class SetupManagerImpl implements InitializingBean, SetupManager {
    private static final Logger logger = Logger.getLogger(SetupManagerImpl.class.getName());

    private final GatewayLicenseManager licenseManager;
    private final IdentityProviderFactory identityProviderFactory;
    private final IdentityProviderConfigManager identityProviderConfigManager;
    private final RoleManager roleManager;
    private final AuditContext auditContext;

    public SetupManagerImpl(final GatewayLicenseManager licenseManager,
                            final IdentityProviderFactory identityProviderFactory,
                            final IdentityProviderConfigManager identityProviderConfigManager,
                            final RoleManager roleManager,
                            final AuditContext context
    ) {
        this.licenseManager = licenseManager;
        this.identityProviderFactory = identityProviderFactory;
        this.identityProviderConfigManager = identityProviderConfigManager;
        this.roleManager = roleManager;
        this.auditContext = context;
    }

    /**
     * Check if this EMS instance has already had initial setup performed.
     * This returns true if any of the following are true:
     * <ul>
     * <li>A valid license is currently installed.</li>
     * <li>At least one internal user currently exists.</li>
     * </ul>
     * @return true if initial setup has been performed per the above.
     * @throws SetupException if there is a problem checking whether any internal users exist
     */
    @Transactional(propagation=SUPPORTS, readOnly=true)
    public boolean isSetupPerformed() throws SetupException  {
        try {
            InternalUserManager internalUserManager = getInternalUserManager();
            return licenseManager.isFeatureEnabled("set:Core") || internalUserManager==null || !internalUserManager.findAllHeaders().isEmpty();
        } catch (FindException e) {
            throw new SetupException(e);
        }
    }


    /**
     * Perform initial setup of this EMS instance.
     * This sets a license and creates the initial administrator user in a single transaction.
     * 
     * @param licenseXml  XML license file to install.  Required.
     * @param initialAdminUsername  username for initial administrator user.  Required.
     * @param initialAdminPassword  password for iniital administrator user.  Required.
     * @throws SetupException if this EMS instance has already been set up.
     */
    @Transactional(propagation=REQUIRED, rollbackFor=Throwable.class)
    public void performInitialSetup(final String licenseXml,
                                    final String initialAdminUsername,
                                    final String initialAdminPassword) throws SetupException {
        try {
            if (isSetupPerformed())
                throw new SetupException("This EMS instance has already been set up.");

            InternalUserManager internalUserManager = getInternalUserManager();
            if ( internalUserManager == null ) throw new SetupException("Unable to access user manager."); 

            Subject subject = Subject.getSubject(AccessController.getContext());
            User temp = new UserBean(initialAdminUsername);
            if ( subject != null ) {
                subject.getPrincipals().add(temp);
            }

            // Create user first, will rollback if license is not valid
            InternalUser user = new InternalUser();
            user.setName(initialAdminUsername);
            user.setLogin(initialAdminUsername);
            user.setCleartextPassword(initialAdminPassword);

            String id = internalUserManager.save(user, Collections.<IdentityHeader>emptySet());
            user.setOid( Long.parseLong(id) );
            if ( subject != null ) {
                subject.getPrincipals().remove(temp);
                subject.getPrincipals().add(user);
            }

            licenseManager.installNewLicense(licenseXml);

            Role adminRole = roleManager.findByUniqueName("Administrator");
            if ( adminRole != null ) {
                adminRole.addAssignedUser( user );
                roleManager.update( adminRole );
            }
        } catch (InvalidPasswordException e) {
            throw new SetupException(e);
        } catch (FindException e) {
            throw new SetupException(e);
        } catch (InvalidLicenseException e) {
            throw new SetupException(e);
        } catch (UpdateException e) {
            throw new SetupException(e);
        } catch (SaveException e) {
            throw new SetupException(e);
        }
    }

    /**
     * Test the given datasource.
     *
     * <p>This will cause failure of the server if the database cannot be accessed.</p>
     *
     * <p>This test avoids an issue with Derby issuing SQL warnings when using the
     * createdb connection option and the database already exists.</p>
     *
     * @param dataSource The datasource to test
     */
    public static void testDataSource( final DataSource dataSource ) {
        Connection connection = null;

        boolean created = true;
        try {
            connection = dataSource.getConnection();
            SQLWarning warning = connection.getWarnings();
            while ( warning != null ) {
                if ( "01J01".equals(warning.getSQLState()) ) {
                    created = false;
                } else {
                    logger.log( Level.WARNING, "SQL Warning: " + warning.getErrorCode() + ", SQLState: " + warning.getSQLState() + ", Message: " + warning.getMessage());
                }
                
                warning = warning.getNextWarning();
            }
        } catch ( SQLException se ) {
            throw new RuntimeException( "Could not connect to database.", se );
        } finally {
            ResourceUtils.closeQuietly(connection);
        }

        if ( created ) {
            logger.config( "Created new database." );
        } else {
            logger.config( "Using existing database." );
        }
    }

    private InternalUserManager getInternalUserManager() throws FindException {
        InternalUserManager internalUserManager = null;

        for ( IdentityProvider identityProvider : identityProviderFactory.findAllIdentityProviders() ) {
            if ( IdentityProviderType.INTERNAL.equals( identityProvider.getConfig().type() ) ) {
                internalUserManager = (InternalUserManager) identityProvider.getUserManager();
                break;
            }
        }

        return internalUserManager;
    }

    /**
     * Add initial identity provider configuration if not present. 
     */
    public void afterPropertiesSet() throws Exception {
        if ( identityProviderConfigManager.findAll().isEmpty() &&
             roleManager.findAll().isEmpty() ) {
            logger.info("Generating initial database configuration.");

            logger.info("Creating configuration for internal identity provider.");
            IdentityProviderConfig config = new IdentityProviderConfig();
            config.setOid(-2);
            config.setTypeVal(1);
            config.setAdminEnabled(true);
            config.setName("Internal Identity Provider");
            config.setDescription("Internal Identity Provider");
            auditContext.setSystem(true);
            long id = identityProviderConfigManager.save(config);
            logger.info("Created configuration for internal identity provider with identifier '" + id + "'.");

            logger.info("Creating configuration for administration role.");
            Role role = new Role();
            role.setName("Administrator");
            role.setDescription("Users assigned to the {0} role have full access to the gateway.");
            role.getPermissions().add(new Permission(role, OperationType.CREATE, EntityType.ANY));
            role.getPermissions().add(new Permission(role, OperationType.READ, EntityType.ANY));
            role.getPermissions().add(new Permission(role, OperationType.UPDATE, EntityType.ANY));
            role.getPermissions().add(new Permission(role, OperationType.DELETE, EntityType.ANY));
            auditContext.setSystem(true);
            id = roleManager.save(role);
            logger.info("Created configuration for administration role with identifier '" + id + "'.");
        }
    }
}
