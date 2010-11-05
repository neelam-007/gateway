package com.l7tech.server.ems.setup;

import com.l7tech.common.io.InetAddressUtil;
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
import com.l7tech.util.Charsets;
import com.l7tech.util.ResourceUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StreamTokenizer;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
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
        if ( "*".equals(ipaddress) ) {            
            serverConfig.putProperty( "em.server.listenaddr", InetAddressUtil.getAnyHostAddress());
        } else {
            serverConfig.putProperty( "em.server.listenaddr", ipaddress );
        }
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
     * Test the given datasource.
     *
     * <p>This will cause failure of the server if the database cannot be accessed.</p>
     *
     * <p>This test avoids an issue with Derby issuing SQL warnings when using the
     * createdb connection option and the database already exists.</p>
     *
     * @param dataSource The datasource to test
     */
    public static void testDataSource( final DataSource dataSource,
                                       final ServerConfig serverConfig,
                                       final Resource[] createScripts,
                                       final Resource[] updateScripts ) {
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

            if ( created ) {
                runScripts( connection, createScripts, false );
            } else if ( serverConfig.getBooleanProperty("em.server.db.updates", false) ) {
                runScripts( connection, updateScripts, serverConfig.getBooleanProperty("em.server.db.updatesDeleted", true) );
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
                            config.setOid(-2);
                            config.setTypeVal(1);
                            config.setAdminEnabled(true);
                            config.setName("Internal Identity Provider");
                            config.setDescription("Internal Identity Provider");
                            long id = identityProviderConfigManager.save(config);
                            logger.info("Created configuration for internal identity provider with identifier '" + id + "'.");

                            logger.info("Creating configuration for administration role.");
                            Role role = new Role();
                            role.setName("Administrator");
                            role.setTag(Role.Tag.ADMIN);
                            role.setDescription("Users assigned to the {0} role have full access to the gateway.");
                            role.getPermissions().add(new Permission(role, OperationType.CREATE, EntityType.ANY));
                            role.getPermissions().add(new Permission(role, OperationType.READ, EntityType.ANY));
                            role.getPermissions().add(new Permission(role, OperationType.UPDATE, EntityType.ANY));
                            role.getPermissions().add(new Permission(role, OperationType.DELETE, EntityType.ANY));
                            id = roleManager.save(role);
                            logger.info("Created configuration for administration role with identifier '" + id + "'.");
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
                            String initialAdminUsername = serverConfig.getProperty("em.admin.user");
                            String initialAdminPassword = serverConfig.getProperty("em.admin.pass");

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
                                user.setOid( Long.parseLong(id) );

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

    private static void runScripts( final Connection connection,
                                    final Resource[] scripts,
                                    final boolean deleteOnComplete ) throws SQLException {
        for ( Resource scriptResource : scripts ) {
            if ( !scriptResource.exists() ) continue;

            StreamTokenizer tokenizer;
            try {
                logger.config("Running DB script '"+scriptResource.getDescription()+"'.");
                tokenizer = new StreamTokenizer( new InputStreamReader(scriptResource.getInputStream(), Charsets.UTF8) );
                tokenizer.eolIsSignificant(false);
                tokenizer.commentChar('-');
                tokenizer.quoteChar('\'');
                tokenizer.wordChars(16, 31);
                tokenizer.wordChars(33, 44);
                tokenizer.wordChars(46,126);

                int token;
                StringBuilder builder = new StringBuilder();
                while( (token = tokenizer.nextToken()) != StreamTokenizer.TT_EOF ) {
                    if ( token == StreamTokenizer.TT_WORD ) {
                        if ( builder.length() > 0 ) {
                            builder.append( " " );
                        }
                        builder.append( tokenizer.sval );
                        if ( tokenizer.sval.endsWith(";") ) {
                            builder.setLength( builder.length()-1 );
                            String sql = builder.toString();
                            builder.setLength( 0 );

                            Statement statement = null;
                            try {
                                statement = connection.createStatement();
                                statement.executeUpdate( sql );
                            } finally {
                                ResourceUtils.closeQuietly( statement );
                            }
                        }
                    }
                }

                if ( deleteOnComplete && scriptResource.getFile()!=null ) {
                    if ( scriptResource.getFile().delete() ) {
                        logger.info( "Deleted DB script '"+scriptResource.getDescription()+"'." );
                    } else {
                        logger.warning( "Deletion failed for DB script '"+scriptResource.getDescription()+"'." );
                    }
                }
            } catch (IOException ioe) {
                logger.log( Level.WARNING, "Error processing DB script.", ioe );
            }
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

    private KeystoreFile newKeystore( final String name, final String format ) {
        KeystoreFile keystoreFile = new KeystoreFile();
        keystoreFile.setName( name );
        keystoreFile.setFormat( format );
        return keystoreFile;
    }
}
