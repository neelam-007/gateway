package com.l7tech.console.util.registry;

import com.l7tech.gateway.common.cluster.ClusterStatusAdmin;
import com.l7tech.common.audit.AuditAdminStub;
import com.l7tech.gateway.common.audit.AuditAdmin;
import com.l7tech.common.log.LogSinkAdminStub;
import com.l7tech.gateway.common.VersionException;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.gateway.common.transport.TransportAdmin;
import com.l7tech.gateway.common.transport.email.*;
import com.l7tech.gateway.common.transport.ftp.FtpAdmin;
import com.l7tech.gateway.common.transport.jms.JmsAdmin;
import com.l7tech.console.security.SecurityProvider;
import com.l7tech.console.util.ConsoleLicenseManager;
import com.l7tech.console.util.Registry;
import com.l7tech.identity.*;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.PolicyPathBuilderFactory;
import com.l7tech.policy.PolicyValidator;
import com.l7tech.policy.Policy;
import com.l7tech.policy.assertion.alert.EmailAlertAssertion;
import com.l7tech.service.*;
import com.l7tech.gateway.common.admin.PolicyAdmin;
import com.l7tech.gateway.common.admin.KerberosAdmin;
import com.l7tech.gateway.common.admin.IdentityAdmin;
import com.l7tech.gateway.common.admin.FolderAdmin;
import com.l7tech.gateway.common.admin.AdminLogin;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.gateway.common.security.rbac.RbacAdmin;
import com.l7tech.gateway.common.security.rbac.Permission;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.TrustedCertAdmin;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.gateway.common.custom.CustomAssertionsRegistrar;
import com.l7tech.gateway.common.schema.SchemaAdmin;
import com.l7tech.gateway.common.log.LogSinkAdmin;
import com.l7tech.identity.IdentityAdminStub;
import com.l7tech.cluster.ClusterStatusAdminStub;

import javax.security.auth.login.LoginException;
import java.net.InetAddress;
import java.net.PasswordAuthentication;
import java.util.Arrays;
import java.util.Collection;
import java.security.cert.X509Certificate;


/**
 * Test, stub registry.
 *
 * @author Emil Marceta
 */
public class RegistryStub extends Registry {

    /**
     * default constructor
     */
    public RegistryStub() {
    }

    @Override
    public boolean isAdminContextPresent() {
        return false;
    }

    @Override
    public AdminLogin getAdminLogin() {
        return null;
    }

    @Override
    public IdentityAdmin getIdentityAdmin() {
        return identityAdmin;
    }

    @Override
    public IdentityProviderConfig getInternalProviderConfig() {
        try {
            return identityAdmin.findIdentityProviderConfigByID(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID);
        } catch ( Exception e ) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return the service managerr
     */
    @Override
    public ServiceAdmin getServiceManager() {
        return null;
    }

    @Override
    public FolderAdmin getFolderAdmin() {
        return null;
    }

    /**
     * @return the jms provider manager
     */
    @Override
    public JmsAdmin getJmsManager() {
        return jmsAdmin;
    }

    /**
     * @return the FTP manager
     */
    @Override
    public FtpAdmin getFtpManager() {
        return ftpAdmin;
    }

    @Override
    public TrustedCertAdmin getTrustedCertManager() {
        return null;
    }

    /**
     * @return the custome assertions registrar
     */
    @Override
    public CustomAssertionsRegistrar getCustomAssertionsRegistrar() {
        return null;
    }

    @Override
    public AuditAdmin getAuditAdmin() {
        return auditAdmin;
    }

    @Override
    public ClusterStatusAdmin getClusterStatusAdmin() {
        return clusterStatusAdmin;
    }

    @Override
    public KerberosAdmin getKerberosAdmin() {
        return null;
    }

    @Override
    public PolicyAdmin getPolicyAdmin() {
        return null;
    }

    @Override
    public RbacAdmin getRbacAdmin() {
        return new RbacAdmin() {
            private final Role role = new Role();
            {
                role.setName("Stub role");
                role.setOid(-777);
                role.setDescription("Fake role for testing");
            }

            @Override
            public Collection<Role> findAllRoles() throws FindException {
                return Arrays.asList(role);
            }

            @Override
            public Role findRoleByPrimaryKey(long oid) throws FindException {
                if (role.getOid() == oid) return role;
                return null;
            }

            @Override
            public Collection<Permission> findCurrentUserPermissions() throws FindException {
                return Arrays.asList(
                        new Permission(role, OperationType.CREATE, EntityType.ANY),
                        new Permission(role, OperationType.READ, EntityType.ANY),
                        new Permission(role, OperationType.UPDATE, EntityType.ANY),
                        new Permission(role, OperationType.DELETE, EntityType.ANY),
                        new Permission(role, OperationType.OTHER, EntityType.ANY)
                );
            }

            @Override
            public Collection<Role> findRolesForUser(User user) throws FindException {
                return findAllRoles();
            }

            @Override
            public long saveRole(Role role) throws SaveException {
                throw new SaveException("Can't save roles in stub mode");
            }

            @Override
            public void deleteRole(Role selectedRole) throws DeleteException {
                throw new DeleteException("Can't delete roles in stub mode");
            }

            @Override
            public EntityHeaderSet<EntityHeader> findEntities(Class<? extends Entity> entityClass) throws FindException {
                return new EntityHeaderSet<EntityHeader>(new EntityHeader(role.getId(), com.l7tech.objectmodel.EntityType.RBAC_ROLE, role.getName(), role.getDescription()));
            }
        };
    }

    @Override
    public TransportAdmin getTransportAdmin() {
        return new TransportAdmin() {
            private final SsgConnector stubAdminConnection = new SsgConnector(
                    2323L,
                    "stub mode admin connector",
                    8443,
                    "https",
                    true,
                    SsgConnector.Endpoint.MESSAGE_INPUT.toString(),
                    SsgConnector.CLIENT_AUTH_OPTIONAL,
                    -1L,
                    null
            );

            @Override
            public Collection<SsgConnector> findAllSsgConnectors() throws FindException {
                return Arrays.asList(stubAdminConnection);
            }

            @Override
            public SsgConnector findSsgConnectorByPrimaryKey(long oid) throws FindException {
                if (stubAdminConnection.getOid() == oid)
                    return stubAdminConnection;
                throw new FindException("no connector found with id " + oid);
            }

            @Override
            public long saveSsgConnector(SsgConnector connector) throws SaveException, UpdateException {
                throw new SaveException("Unable to save any connectors in stub mode");
            }

            @Override
            public void deleteSsgConnector(long oid) throws DeleteException, FindException {
                throw new DeleteException("Unable to delete any connectors in stub mode");
            }

            @Override
            public String[] getAllCipherSuiteNames() {
                return new String[0];
            }

            @Override
            public String[] getDefaultCipherSuiteNames() {
                return new String[0];
            }

            @Override
            public InetAddress[] getAvailableBindAddresses() {
                return null;
            }
        };
    }

    @Override
    public EmailListenerAdmin getEmailListenerAdmin() {
        return new EmailListenerAdmin() {
            private final EmailListener emailListener = new EmailListener(2468L,
                                                                          "testuser@layer7tech.com",
                                                                          "mail.layer7tech.com",
                                                                          143,
                                                                          EmailServerType.IMAP,
                                                                          true,
                                                                          true,
                                                                          "testuser",
                                                                          "password",
                                                                          "Inbox",
                                                                          5,
                                                                          true,
                                                                          "test-node",
                                                                          System.currentTimeMillis(),
                                                                          1L,
                                                                          "properties");
            
            @Override
            public EmailListener findEmailListenerByPrimaryKey(long oid) throws FindException {
                if(oid == 2468L) {
                    return emailListener;
                } else {
                    throw new FindException("no email listener found with id " + oid);
                }
            }

            @Override
            public Collection<EmailListener> findAllEmailListeners() throws FindException {
                return Arrays.asList(emailListener);
            }

            @Override
            public long saveEmailListener(EmailListener emailListener) throws SaveException, UpdateException {
                throw new SaveException("Unable to save any email listeners in stub mode");
            }

            @Override
            public void deleteEmailListener(long oid) throws DeleteException, FindException {
                throw new DeleteException("Unable to delete any email listeners in stub mode");
            }

            @Override
            public IMAPFolder getIMAPFolderList(String hostname, int port, String username, String password, boolean useSSL) {
                return null;
            }

            @Override
            public boolean testEmailAccount(EmailServerType serverType,
                                            String hostname,
                                            int port,
                                            String username,
                                            String password,
                                            boolean useSSL,
                                            String folderName)
            {
                return true;
            }
        };
    }

    @Override
    public SecurityProvider getSecurityProvider() {
        return new SecurityProvider() {
            @Override
            public void login(PasswordAuthentication creds, String host, boolean validateHost, String newPassword)
                    throws LoginException, VersionException, InvalidPasswordException {                
            }

            @Override
            public void login(String sessionId, String host) throws LoginException, VersionException {
            }

            @Override
            public void changePassword(PasswordAuthentication auth, PasswordAuthentication newAuth) throws LoginException {
            }

            @Override
            public void logoff() {
            }

            @Override
            public void acceptServerCertificate(X509Certificate certificate) {
            }
        };
    }

    @Override
    public PolicyValidator getPolicyValidator() {
        return  null;
    }

    @Override
    public GuidBasedEntityManager<Policy> getPolicyFinder() {
        return  null;
    }

    @Override
    public PolicyPathBuilderFactory getPolicyPathBuilderFactory() {
        return  null;
    }

    @Override
    public SchemaAdmin getSchemaAdmin() {
        return null;
    }

    @Override
    public ConsoleLicenseManager getLicenseManager() {
        return licenseManager;
    }

    @Override
    public LogSinkAdmin getLogSinkAdmin() {
        return logSinkAdmin;
    }

    @Override
    public EmailAdmin getEmailAdmin() {
        return new EmailAdmin() {
            @Override
            public void testSendEmail(EmailAlertAssertion eaa) throws EmailTestException {
            }

            @Override
            public void testSendEmail(String toAddr, String ccAddr, String bccAddr, String fromAddr, String subject,
                                      String host, int port, String base64Message, EmailAlertAssertion.Protocol protocol,
                                      boolean authenticate, String authUsername, String authPassword) throws EmailTestException {
            }
        };
    }

    private IdentityAdmin identityAdmin = new IdentityAdminStub();
    private JmsAdmin jmsAdmin = new JmsAdminStub();
    private FtpAdmin ftpAdmin = new FtpAdminStub();
    private AuditAdmin auditAdmin = new AuditAdminStub();
    private ClusterStatusAdmin clusterStatusAdmin = new ClusterStatusAdminStub();
    private ConsoleLicenseManager licenseManager =  null;//new TestLicenseManager();
    private LogSinkAdmin logSinkAdmin = new LogSinkAdminStub();
}
