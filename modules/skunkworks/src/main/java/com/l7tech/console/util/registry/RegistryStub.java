package com.l7tech.console.util.registry;

import com.l7tech.gateway.common.cluster.ClusterStatusAdmin;
//import com.l7tech.server.TestLicenseManager;
import com.l7tech.common.audit.AuditAdminStub;
import com.l7tech.gateway.common.audit.AuditAdmin;
import com.l7tech.common.io.PortRange;
import com.l7tech.common.log.LogSinkAdminStub;
import com.l7tech.gateway.common.VersionException;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.gateway.common.transport.TransportAdmin;
import com.l7tech.gateway.common.transport.email.*;
import com.l7tech.gateway.common.transport.ftp.FtpAdmin;
import com.l7tech.gateway.common.transport.jms.JmsAdmin;
import com.l7tech.util.Pair;
import com.l7tech.util.Triple;
import com.l7tech.console.security.SecurityProvider;
import com.l7tech.console.util.ConsoleLicenseManager;
import com.l7tech.console.util.Registry;
import com.l7tech.identity.*;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.PolicyPathBuilderFactory;
import com.l7tech.policy.PolicyValidator;
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
import java.util.Collections;
import java.security.cert.X509Certificate;


/**
 * Test, stub registry.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class RegistryStub extends Registry {
    private Role role;
//    private final PolicyManagerStub policyManager = new PolicyManagerStub();
//    private final PolicyPathBuilderFactory policyPathBuilderFactory = new PolicyPathBuilderFactory(policyManager);
//    private final DefaultPolicyValidator policyValidator = new DefaultPolicyValidator(policyManager, policyPathBuilderFactory);

    /**
     * default constructor
     */
    public RegistryStub() {
//        serviceAdmin.setPolicyValidator(policyValidator);
//        serviceAdmin.setServiceManager(new ServiceManagerStub(policyManager));
    }

    public boolean isAdminContextPresent() {
        return false;//serviceAdmin != null;
    }

    public AdminLogin getAdminLogin() {
        return null;
    }

    public IdentityAdmin getIdentityAdmin() {
        return identityAdmin;
    }

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
    public ServiceAdmin getServiceManager() {
        return null;//serviceAdmin;
    }

    public FolderAdmin getFolderAdmin() {
        return null;
    }

    /**
     * @return the jms provider manager
     */
    public JmsAdmin getJmsManager() {
        return jmsAdmin;
    }

    /**
     * @return the FTP manager
     */
    public FtpAdmin getFtpManager() {
        return ftpAdmin;
    }

    public TrustedCertAdmin getTrustedCertManager() {
        return trustedCertAdmin;
    }

    /**
     * @return the custome assertions registrar
     */
    public CustomAssertionsRegistrar getCustomAssertionsRegistrar() {
        return null;//customAssertionsRegistrar;
    }

    public AuditAdmin getAuditAdmin() {
        return auditAdmin;
    }

    public ClusterStatusAdmin getClusterStatusAdmin() {
        return clusterStatusAdmin;
    }

    public KerberosAdmin getKerberosAdmin() {
        return null;
    }

    public PolicyAdmin getPolicyAdmin() {
        return null;
    }

    public RbacAdmin getRbacAdmin() {
        return new RbacAdmin() {
            private final Role role = new Role();
            {
                role.setName("Stub role");
                role.setOid(-777);
                role.setDescription("Fake role for testing");
            }

            public Collection<Role> findAllRoles() throws FindException {
                return Arrays.asList(role);
            }

            public Role findRoleByPrimaryKey(long oid) throws FindException {
                if (role.getOid() == oid) return role;
                return null;
            }

            public Collection<Permission> findCurrentUserPermissions() throws FindException {
                return Arrays.asList(
                        new Permission(role, OperationType.CREATE, EntityType.ANY),
                        new Permission(role, OperationType.READ, EntityType.ANY),
                        new Permission(role, OperationType.UPDATE, EntityType.ANY),
                        new Permission(role, OperationType.DELETE, EntityType.ANY),
                        new Permission(role, OperationType.OTHER, EntityType.ANY)
                );
            }

            public Collection<Role> findRolesForUser(User user) throws FindException {
                return findAllRoles();
            }

            public long saveRole(Role role) throws SaveException {
                throw new SaveException("Can't save roles in stub mode");
            }

            public void deleteRole(Role selectedRole) throws DeleteException {
                throw new DeleteException("Can't delete roles in stub mode");
            }

            public EntityHeaderSet<EntityHeader> findEntities(Class<? extends Entity> entityClass) throws FindException {
                return new EntityHeaderSet<EntityHeader>(new EntityHeader(role.getId(), com.l7tech.objectmodel.EntityType.RBAC_ROLE, role.getName(), role.getDescription()));
            }
        };
    }

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

            public Collection<SsgConnector> findAllSsgConnectors() throws FindException {
                return Arrays.asList(stubAdminConnection);
            }

            public SsgConnector findSsgConnectorByPrimaryKey(long oid) throws FindException {
                if (stubAdminConnection.getOid() == oid)
                    return stubAdminConnection;
                throw new FindException("no connector found with id " + oid);
            }

            public long saveSsgConnector(SsgConnector connector) throws SaveException, UpdateException {
                throw new SaveException("Unable to save any connectors in stub mode");
            }

            public void deleteSsgConnector(long oid) throws DeleteException, FindException {
                throw new DeleteException("Unable to delete any connectors in stub mode");
            }

            public String[] getAllCipherSuiteNames() {
                return new String[0];
            }

            public String[] getDefaultCipherSuiteNames() {
                return new String[0];
            }

            public InetAddress[] getAvailableBindAddresses() {
                return null;
            }

            public Collection<Triple<Long, PortRange, String>> findAllPortConflicts() throws FindException {
                return Collections.emptyList();
            }

            public Collection<Pair<PortRange, String>> findPortConflicts(SsgConnector unsavedConnector) throws FindException {
                return Collections.emptyList();
            }
        };
    }

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
                                                                          1L);
            
            public EmailListener findEmailListenerByPrimaryKey(long oid) throws FindException {
                if(oid == 2468L) {
                    return emailListener;
                } else {
                    throw new FindException("no email listener found with id " + oid);
                }
            }

            public Collection<EmailListener> findAllEmailListeners() throws FindException {
                return Arrays.asList(emailListener);
            }

            public long saveEmailListener(EmailListener emailListener) throws SaveException, UpdateException {
                throw new SaveException("Unable to save any email listeners in stub mode");
            }

            public void deleteEmailListener(long oid) throws DeleteException, FindException {
                throw new DeleteException("Unable to delete any email listeners in stub mode");
            }

            public IMAPFolder getIMAPFolderList(String hostname, int port, String username, String password, boolean useSSL) {
                return null;
            }

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

    public SecurityProvider getSecurityProvider() {
        return new SecurityProvider() {
            public void login(PasswordAuthentication creds, String host, boolean validateHost, String newPassword)
                    throws LoginException, VersionException, InvalidPasswordException {                
            }

            public void login(String sessionId, String host) throws LoginException, VersionException {
            }

            public void changePassword(PasswordAuthentication auth, PasswordAuthentication newAuth) throws LoginException {
            }

            public void logoff() {                
            }

            public void acceptServerCertificate(X509Certificate certificate) {                
            }
        };
    }

    public PolicyValidator getPolicyValidator() {
        return  null;//policyValidator;
    }

    public PolicyPathBuilderFactory getPolicyPathBuilderFactory() {
        return  null;//policyPathBuilderFactory;
    }

    public SchemaAdmin getSchemaAdmin() {
        return null;
    }

    public ConsoleLicenseManager getLicenseManager() {
        return licenseManager;
    }

    public LogSinkAdmin getLogSinkAdmin() {
        return logSinkAdmin;
    }

    public EmailAdmin getEmailAdmin() {
        return new EmailAdmin() {
            public void testSendEmail(EmailAlertAssertion eaa) throws EmailTestException {
                return;
            }

            public void testSendEmail(String toAddr, String ccAddr, String bccAddr, String fromAddr, String subject,
                                      String host, int port, String base64Message, EmailAlertAssertion.Protocol protocol,
                                      boolean authenticate, String authUsername, String authPassword) throws EmailTestException {
                return;
            }
        };
    }

    //StubDataStore dataStore = StubDataStore.defaultStore();

    private IdentityAdmin identityAdmin = new IdentityAdminStub();
    //private ServiceAdminStub serviceAdmin = new ServiceAdminStub();
    private JmsAdmin jmsAdmin = new JmsAdminStub();
    private FtpAdmin ftpAdmin = new FtpAdminStub();
    //private CustomAssertionsRegistrar customAssertionsRegistrar = new CustomAssertionsRegistrarStub();
    private AuditAdmin auditAdmin = new AuditAdminStub();
    private ClusterStatusAdmin clusterStatusAdmin = new ClusterStatusAdminStub();
    private TrustedCertAdmin trustedCertAdmin;
    private ConsoleLicenseManager licenseManager =  null;//new TestLicenseManager();
    private LogSinkAdmin logSinkAdmin = new LogSinkAdminStub();
}
