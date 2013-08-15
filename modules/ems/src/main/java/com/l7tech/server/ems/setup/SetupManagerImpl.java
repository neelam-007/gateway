package com.l7tech.server.ems.setup;

import com.l7tech.util.InetAddressUtil;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.Permission;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.*;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.audit.AuditContextUtils;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.ems.enterprise.EnterpriseFolder;
import com.l7tech.server.ems.enterprise.EnterpriseFolderManager;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.identity.internal.InternalUserManager;
import com.l7tech.server.security.keystore.*;
import com.l7tech.server.security.rbac.RoleManager;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Collections;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.springframework.transaction.annotation.Propagation.REQUIRED;

/**
 * Encapsulates behavior for initial setup of a new EMS instance.
 */
@Transactional(propagation=REQUIRED, rollbackFor=Throwable.class)
public class SetupManagerImpl implements InitializingBean, SetupManager {

    //- PUBLIC

    public SetupManagerImpl(final ServerConfig serverConfig,
                            final PlatformTransactionManager transactionManager,
                            final IdentityProviderFactory identityProviderFactory,
                            final IdentityProviderConfigManager identityProviderConfigManager,
                            final RoleManager roleManager,
                            final EnterpriseFolderManager enterpriseFolderManager,
                            final KeystoreFileManager keystoreFileManager,
                            final ClusterPropertyManager clusterPropertyManager
    ) {
        this.serverConfig = serverConfig;
        this.transactionManager = transactionManager;
        this.identityProviderFactory = identityProviderFactory;
        this.identityProviderConfigManager = identityProviderConfigManager;
        this.roleManager = roleManager;
        this.enterpriseFolderManager = enterpriseFolderManager;
        this.keystoreFileManager = keystoreFileManager;
        this.clusterPropertyManager = clusterPropertyManager;
    }

    @Override
    public String getEsmId() {
        return serverConfig.getProperty( "em.server.id" );
    }

    @Override
    public void deleteLicense() throws DeleteException {
        try {
            ClusterProperty licProp = clusterPropertyManager.findByUniqueName("license");
            if (licProp != null) {
                clusterPropertyManager.delete(licProp);
            }
         } catch (FindException ex) {
            logger.log( Level.WARNING, "Error accessing license for deletion.", ex );
         }
    }

    @Override
    public void configureListener( final String ipaddress, final int port ) throws SetupException {
        // update server settings
        serverConfig.putProperty( "em.server.listenport", Integer.toString(port) );
        serverConfig.putProperty( "em.server.listenaddr", "*".equals(ipaddress) ? InetAddressUtil.getAnyHostAddress() : ipaddress);
    }

    @Override
    public void setSessionTimeout(int sessionTimeout) throws SetupException {
        try {
            final String newValue = Integer.toString(sessionTimeout) + "m";
           ClusterProperty prop = clusterPropertyManager.findByUniqueName("session.timeout");
           if ( prop == null ) {
               clusterPropertyManager.save( new ClusterProperty( "session.timeout", newValue ) );
           } else {
               prop.setValue( newValue );
               clusterPropertyManager.update( prop );
           }
        } catch ( ObjectModelException ome ) {
            throw new SetupException( "Error saving session timeout.", ome );
        }
    }

    /**
     * Add initial identity provider configuration if not present.
     */
    @Override
    public void afterPropertiesSet() {
        final boolean wasSystem = AuditContextUtils.isSystem();
        try {
            AuditContextUtils.setSystem(true);

            TransactionTemplate template = new TransactionTemplate(transactionManager);
            template.execute( new TransactionCallbackWithoutResult(){
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                    try {
                        if ( clusterPropertyManager.findByUniqueName("esm.id") == null ) {
                            String esmId = UUID.randomUUID().toString().replaceAll("-","");
                            serverConfig.putProperty( "em.server.id", esmId );
                            clusterPropertyManager.save( new ClusterProperty( "esm.id", esmId ) );
                        }

                        if ( identityProviderConfigManager.findAll().isEmpty() &&
                             roleManager.findAll().isEmpty() ) {
                            logger.info("Generating initial database configuration.");

                            logger.info("Creating configuration for internal identity provider.");
                            IdentityProviderConfig config = new IdentityProviderConfig();
                            config.setGoid(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID);
                            config.setTypeVal(1);
                            config.setAdminEnabled(true);
                            config.setName("Internal Identity Provider");
                            config.setDescription("Internal Identity Provider");
                            Goid goid = identityProviderConfigManager.save(config);
                            logger.info("Created configuration for internal identity provider with identifier '" + goid + "'.");

                            logger.info("Creating configuration for administration role.");
                            Role role = new Role();
                            role.setName("Administrator");
                            role.setTag(Role.Tag.ADMIN);
                            role.setDescription("Users assigned to the {0} role have full access to the gateway.");
                            role.getPermissions().add(new Permission(role, OperationType.CREATE, EntityType.ANY));
                            role.getPermissions().add(new Permission(role, OperationType.READ, EntityType.ANY));
                            role.getPermissions().add(new Permission(role, OperationType.UPDATE, EntityType.ANY));
                            role.getPermissions().add(new Permission(role, OperationType.DELETE, EntityType.ANY));
                            goid = roleManager.save(role);
                            logger.info("Created configuration for administration role with identifier '" + goid + "'.");
                        }
                    } catch ( Exception e ) {
                        transactionStatus.setRollbackOnly();
                        throw new RuntimeException( "Error during initial setup.", e );
                    }
                }
            });

            // separate transaction since we want the provider / role to be persisted before we run this.
            template.execute( new TransactionCallbackWithoutResult(){
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                    try {
                        InternalUserManager internalUserManager = getInternalUserManager();
                        if ( internalUserManager != null ) {
                            String initialAdminUsername = serverConfig.getProperty( "em.admin.user" );
                            String initialAdminPassword = serverConfig.getProperty( "em.admin.pass" );

                            boolean create = false;
                            if ( initialAdminUsername != null && initialAdminUsername.trim().length() > 0 &&
                                 initialAdminPassword != null && initialAdminPassword.trim().length() > 0) {
                                create = internalUserManager.findByLogin(initialAdminUsername) == null;
                            }

                            if ( create ) {
                                logger.info("Creating administative user with account '" + initialAdminUsername + "'.");
                                InternalUser user = new InternalUser();
                                user.setName(initialAdminUsername);
                                user.setLogin(initialAdminUsername);
                                user.setHashedPassword(initialAdminPassword);

                                String id = internalUserManager.save(user, Collections.<IdentityHeader>emptySet());
                                user.setGoid( Goid.parseGoid(id) );

                                Role adminRole = roleManager.findByTag(Role.Tag.ADMIN);
                                if ( adminRole != null ) {
                                    adminRole.addAssignedUser( user );
                                    roleManager.update( adminRole );
                                }
                            }
                        } else {
                            logger.warning("User manager not found during initialization.");
                        }

                        if ( keystoreFileManager.findAll().isEmpty() ) {
                            keystoreFileManager.save( newKeystore("Software DB", "sdb.pkcs12") );
                        }

                        if ( enterpriseFolderManager.findAll().isEmpty() ) {
                            logger.info("Creating root folder with name \"" + EnterpriseFolder.DEFAULT_ROOT_FOLDER_NAME + "\".");
                            enterpriseFolderManager.create(EnterpriseFolder.DEFAULT_ROOT_FOLDER_NAME, (EnterpriseFolder)null);
                        }
                    } catch ( Exception e ) {
                        transactionStatus.setRollbackOnly();
                        throw new RuntimeException( "Error during initial setup.", e );
                    }
                }
            });
        } finally {
            AuditContextUtils.setSystem( wasSystem );
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(SetupManagerImpl.class.getName());

    private final ServerConfig serverConfig;
    private final PlatformTransactionManager transactionManager;
    private final IdentityProviderFactory identityProviderFactory;
    private final IdentityProviderConfigManager identityProviderConfigManager;
    private final RoleManager roleManager;
    private final EnterpriseFolderManager enterpriseFolderManager;
    private final KeystoreFileManager keystoreFileManager;
    private final ClusterPropertyManager clusterPropertyManager;

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

    private KeystoreFile newKeystore( final String name, final String format ) {
        KeystoreFile keystoreFile = new KeystoreFile();
        keystoreFile.setName( name );
        keystoreFile.setFormat( format );
        return keystoreFile;
    }
}
