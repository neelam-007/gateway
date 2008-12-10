package com.l7tech.server.ems;

import com.l7tech.gateway.common.InvalidLicenseException;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.Permission;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.identity.*;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.*;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.UpdatableLicenseManager;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.audit.AuditContext;
import com.l7tech.server.ems.enterprise.EnterpriseFolder;
import com.l7tech.server.ems.enterprise.EnterpriseFolderManager;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.identity.internal.InternalUserManager;
import com.l7tech.server.security.keystore.KeystoreFile;
import com.l7tech.server.security.keystore.KeystoreFileManager;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.security.keystore.SsgKeyFinder;
import com.l7tech.server.security.keystore.SsgKeyStore;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ResourceUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import static org.springframework.transaction.annotation.Propagation.REQUIRED;
import static org.springframework.transaction.annotation.Propagation.SUPPORTS;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import javax.security.auth.Subject;
import javax.security.auth.x500.X500Principal;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StreamTokenizer;
import java.security.AccessController;
import java.security.PrivateKey;
import java.security.KeyStoreException;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Encapsulates behavior for initial setup of a new EMS instance.
 */
@Transactional(propagation=REQUIRED, rollbackFor=Throwable.class)
public class SetupManagerImpl implements InitializingBean, SetupManager {
    private static final Logger logger = Logger.getLogger(SetupManagerImpl.class.getName());

    private final ServerConfig serverConfig;
    private final PlatformTransactionManager transactionManager;
    private final UpdatableLicenseManager licenseManager;
    private final IdentityProviderFactory identityProviderFactory;
    private final IdentityProviderConfigManager identityProviderConfigManager;
    private final RoleManager roleManager;
    private final EnterpriseFolderManager enterpriseFolderManager;
    private final AuditContext auditContext;
    private final KeystoreFileManager keystoreFileManager;
    private final ClusterPropertyManager clusterPropertyManager;
    private SsgKeyStoreManager keyStoreManager;

    public SetupManagerImpl(final ServerConfig serverConfig,
                            final PlatformTransactionManager transactionManager,
                            final UpdatableLicenseManager licenseManager,
                            final IdentityProviderFactory identityProviderFactory,
                            final IdentityProviderConfigManager identityProviderConfigManager,
                            final RoleManager roleManager,
                            final EnterpriseFolderManager enterpriseFolderManager,
                            final AuditContext context,
                            final KeystoreFileManager keystoreFileManager,
                            final ClusterPropertyManager clusterPropertyManager
    ) {
        this.serverConfig = serverConfig;
        this.transactionManager = transactionManager;
        this.licenseManager = licenseManager;
        this.identityProviderFactory = identityProviderFactory;
        this.identityProviderConfigManager = identityProviderConfigManager;
        this.roleManager = roleManager;
        this.enterpriseFolderManager = enterpriseFolderManager;
        this.auditContext = context;
        this.keystoreFileManager = keystoreFileManager;
        this.clusterPropertyManager = clusterPropertyManager;
    }

    public void setKeyStoreManager( final SsgKeyStoreManager keyStoreManager ) {
        this.keyStoreManager = keyStoreManager;
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
    @Override
    @Transactional(propagation=SUPPORTS, readOnly=true)
    public boolean isSetupPerformed() throws SetupException  {
        boolean setup = true;
        try {
            InternalUserManager internalUserManager = getInternalUserManager();
            if ( internalUserManager==null || internalUserManager.findAllHeaders().isEmpty() ) {
                setup = false;
            } else {
                licenseManager.getCurrentLicense(); // gets license only if valid
            }
        } catch (FindException e) {
            throw new SetupException(e);
        } catch (InvalidLicenseException ile) {
            logger.warning("License is not valid '"+ ExceptionUtils.getDebugException(ile) +"'.");
            setup = false;
        }
        return setup;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
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
            serverConfig.putProperty( "em.server.listenaddr", "0.0.0.0" );
        } else {
            serverConfig.putProperty( "em.server.listenaddr", ipaddress );
        }
    }

    @Override
    public String saveSsl( final PrivateKey key, final X509Certificate[] certificateChain ) throws SetupException {
        try {
            // save key
            String alias = findUnusedAlias("SSL");
            SsgKeyStore sks = findFirstMutableKeystore();
            SsgKeyEntry entry = new SsgKeyEntry( sks.getOid(), alias, certificateChain, key );
            sks.storePrivateKeyEntry( entry, false );
            return alias;
        } catch ( KeyStoreException kse ) {
            throw new SetupException( "Error during keystore configuration.", kse );
        } catch ( IOException ioe ) {
            throw new SetupException( "Error during keystore configuration.", ioe );
        }
    }

    @Override
    public String generateSsl( final String hostname ) throws SetupException {
        try {
            // generate key and save
            String alias = findUnusedAlias("SSL");
            SsgKeyStore sks = findFirstMutableKeystore();
            generateKeyPair( hostname, sks, alias );
            return alias;
        } catch ( IOException ioe ) {
            throw new SetupException( "Error during keystore configuration.", ioe );
        }
    }

    @Override
    public void setSslAlias( final String alias ) throws SetupException {
        try {
            SsgKeyStore sks = findFirstMutableKeystore();
            configureAsDefaultSslCert(sks, alias);
        } catch ( IOException ioe ) {
            throw new SetupException( "Error during keystore configuration.", ioe );
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
    @Override
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
    public static void testDataSource( final DataSource dataSource, final Resource[] scripts ) {
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
                runScripts( connection, scripts );
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

    private static void runScripts( final Connection connection, final Resource[] scripts ) throws SQLException {
        for ( Resource scriptResource : scripts ) {
            StreamTokenizer tokenizer;

            try {
                logger.config("Running DB create script '"+scriptResource.getDescription()+"'.");
                tokenizer = new StreamTokenizer( new InputStreamReader(scriptResource.getInputStream(), "UTF-8") );
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

    /**
     * Add initial identity provider configuration if not present.
     */
    @Override
    public void afterPropertiesSet() {
        final String[] uuidHolder = new String[1];

        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.execute( new TransactionCallbackWithoutResult(){
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                try {
                    if ( clusterPropertyManager.findByUniqueName("esm.id") == null ) {
                        uuidHolder[0] = UUID.randomUUID().toString();
                        clusterPropertyManager.save( new ClusterProperty( "esm.id", uuidHolder[0] ) );
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
                        auditContext.setSystem(true);
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
                        auditContext.setSystem(true);
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
                        final boolean wasSystem = auditContext.isSystem();
                        auditContext.setSystem(true);
                        try {
                            keystoreFileManager.save( newKeystore("Software DB", "sdb.pkcs12") );
                        } finally {
                            auditContext.setSystem(wasSystem);
                        }
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

        if ( uuidHolder[0] != null ) {
            serverConfig.putProperty( "em.server.id", uuidHolder[0] ); // work around for application events not yet available
        }
    }

    private KeystoreFile newKeystore( final String name, final String format ) {
        KeystoreFile keystoreFile = new KeystoreFile();
        keystoreFile.setName( name );
        keystoreFile.setFormat( format );
        return keystoreFile;
    }

    private void generateKeyPair(final String hostname, final SsgKeyStore sks, final String alias) throws IOException {
        X500Principal dn = new X500Principal("cn=" + hostname);
        try {
            Future<X509Certificate> job = sks.generateKeyPair(alias, dn, 1024, 365 * 10, false);
            job.get();
        } catch (GeneralSecurityException e) {
            throw new IOException("Unable to create initial default SSL key: " + ExceptionUtils.getMessage(e), e);
        } catch (ExecutionException e) {
            throw new IOException("Unable to create initial default SSL key: " + ExceptionUtils.getMessage(e), e);
        } catch (InterruptedException e) {
            throw new IOException("Unable to create initial default SSL key: " + ExceptionUtils.getMessage(e), e);
        }
    }

    private SsgKeyEntry configureAsDefaultSslCert(SsgKeyStore sks, String alias) throws IOException {
        try {
            SsgKeyEntry entry = sks.getCertificateChain(alias);
            String name = serverConfig.getClusterPropertyName(ServerConfig.PARAM_KEYSTORE_DEFAULT_SSL_KEY);
            if (name == null)
                throw new IOException("Unable to configure default SSL key: no cluster property defined for ServerConfig property " + ServerConfig.PARAM_KEYSTORE_DEFAULT_SSL_KEY);
            String value = entry.getKeystoreId() + ":" + alias;
            clusterPropertyManager.putProperty(name, value);
            return entry;
        } catch (KeyStoreException e) {
            throw new IOException("Unable to find default SSL key: " + ExceptionUtils.getMessage(e), e);
        } catch (FindException e) {
            throw new IOException("Unable to configure default SSL key: " + ExceptionUtils.getMessage(e), e);
        } catch (UpdateException e) {
            throw new IOException("Unable to configure default SSL key: " + ExceptionUtils.getMessage(e), e);
        } catch (SaveException e) {
            throw new IOException("Unable to configure default SSL key: " + ExceptionUtils.getMessage(e), e);
        }
    }

    private SsgKeyStore findFirstMutableKeystore() throws IOException {
        SsgKeyStore sks = null;
        try {
            List<SsgKeyFinder> got = keyStoreManager.findAll();
            for (SsgKeyFinder ssgKeyFinder : got) {
                if (ssgKeyFinder.isMutable()) {
                    sks = ssgKeyFinder.getKeyStore();
                    break;
                }
            }
            if (sks == null)
                throw new IOException("No mutable keystores found in which to store default SSL key");
        } catch (FindException e) {
            throw new IOException("Unable to find keystore in which to store default SSL key: " + ExceptionUtils.getMessage(e), e);
        } catch (KeyStoreException e) {
            throw new IOException("Unable to find keystore in which to store default SSL key: " + ExceptionUtils.getMessage(e), e);
        }
        return sks;
    }

    private String findUnusedAlias(String baseAlias) throws IOException {
        String alias = baseAlias;
        int count = 1;
        while (aliasAlreadyUsed(alias)) {
            alias = baseAlias + (count++);
        }
        return alias;
    }

    private boolean aliasAlreadyUsed(String alias) throws IOException {
        try {
            keyStoreManager.lookupKeyByKeyAlias(alias, -1);
            return true;
        } catch (ObjectNotFoundException e) {
            return false;
        } catch (FindException e) {
            throw new IOException("Unable to check if alias \"" + alias + "\" is already used: " + ExceptionUtils.getMessage(e), e);
        } catch (KeyStoreException e) {
            throw new IOException("Unable to check if alias \"" + alias + "\" is already used: " + ExceptionUtils.getMessage(e), e);
        }
    }

}
