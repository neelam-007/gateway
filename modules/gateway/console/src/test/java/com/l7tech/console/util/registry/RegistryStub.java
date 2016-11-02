package com.l7tech.console.util.registry;

import com.l7tech.common.io.PortRanges;
import com.l7tech.console.TrustedCertAdminStub;
import com.l7tech.console.security.SecurityProvider;
import com.l7tech.console.util.ConsoleLicenseManager;
import com.l7tech.console.util.EntityNameResolver;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.VersionException;
import com.l7tech.gateway.common.admin.*;
import com.l7tech.gateway.common.audit.AuditAdmin;
import com.l7tech.gateway.common.audit.AuditAdminStub;
import com.l7tech.gateway.common.cassandra.CassandraConnectionManagerAdmin;
import com.l7tech.gateway.common.cluster.ClusterStatusAdmin;
import com.l7tech.gateway.common.cluster.ClusterStatusAdminStub;
import com.l7tech.gateway.common.custom.CustomAssertionsRegistrar;
import com.l7tech.gateway.common.jdbc.JdbcAdmin;
import com.l7tech.gateway.common.log.LogSinkAdmin;
import com.l7tech.gateway.common.log.LogSinkAdminStub;
import com.l7tech.gateway.common.resources.ResourceAdmin;
import com.l7tech.gateway.common.security.TrustedCertAdmin;
import com.l7tech.gateway.common.security.rbac.*;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.gateway.common.siteminder.SiteMinderAdmin;
import com.l7tech.gateway.common.solutionkit.SolutionKitAdmin;
import com.l7tech.gateway.common.task.ScheduledTaskAdmin;
import com.l7tech.gateway.common.transport.*;
import com.l7tech.gateway.common.transport.email.*;
import com.l7tech.gateway.common.transport.firewall.SsgFirewallRule;
import com.l7tech.gateway.common.transport.ftp.FtpAdmin;
import com.l7tech.gateway.common.transport.ftp.FtpAdminStub;
import com.l7tech.gateway.common.transport.jms.JmsAdmin;
import com.l7tech.gateway.common.transport.jms.JmsAdminStub;
import com.l7tech.gateway.common.workqueue.WorkQueueManagerAdmin;
import com.l7tech.identity.Group;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.*;
import com.l7tech.policy.assertion.alert.EmailAlertAssertion;
import com.l7tech.util.Functions;
import com.l7tech.util.InetAddressUtil;
import com.l7tech.util.Option;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;

import javax.security.auth.login.LoginException;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.net.PasswordAuthentication;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static com.l7tech.util.Option.none;


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
        return adminContextPresent;
    }

    public void setAdminContextPresent(boolean adminContextPresent) {
        this.adminContextPresent = adminContextPresent;
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
            return identityAdmin.findIdentityProviderConfigByID(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID);
        } catch ( Exception e ) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return the service managerr
     */
    @Override
    public ServiceAdmin getServiceManager() {
        return serviceManager;
    }

    public void setServiceManager(final ServiceAdmin serviceManager) {
        this.serviceManager = serviceManager;
    }

    @Override
    public FolderAdmin getFolderAdmin() {
        return folderAdmin;
    }

    public void setFolderAdmin(final FolderAdmin folderAdmin) {
        this.folderAdmin = folderAdmin;
    }

    /**
     * @return the jms provider manager
     */
    @Override
    public JmsAdmin getJmsManager() {
        return jmsAdmin;
    }

    @Override
    public JdbcAdmin getJdbcConnectionAdmin() {
        return null;
    }


    @Override
    public CassandraConnectionManagerAdmin getCassandraConnectionAdmin() {
        return null;
    }

    @Override
    public SiteMinderAdmin getSiteMinderConfigurationAdmin(){
        return null;
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
        return trustedCertAdmin;
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
        return policyAdmin;
    }

    public void setPolicyAdmin(final PolicyAdmin policyAdmin) {
        this.policyAdmin = policyAdmin;
    }

    @Override
    public RbacAdmin getRbacAdmin() {
        return new RbacAdmin() {
            private final Role role = new Role();
            {
                role.setName("Stub role");
                role.setGoid(new Goid (0, -777L));
                role.setDescription("Fake role for testing");
            }

            @Override
            public Collection<Role> findAllRoles() throws FindException {
                return Arrays.asList(role);
            }

            @Override
            public Role findRoleByPrimaryKey(Goid goid) throws FindException {
                if (role.getGoid() != null &&
                    role.getGoid().equals(goid))
                    return role;
                return null;
            }

            public Collection<EntityHeader> findAllRoleHeaders() throws FindException {
                return null;
            }

            @NotNull
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

            @NotNull
            @Override
            public Map<String, EntityProtectionInfo> findProtectedEntities() throws FindException {
                return Collections.emptyMap();
            }

            @NotNull
            @Override
            public Pair<Collection<Permission>, Map<String, EntityProtectionInfo>> findCurrentUserPermissionsAndProtectedEntities() throws FindException {
                final Collection<Permission> permissions = findCurrentUserPermissions();
                final Map<String, EntityProtectionInfo> protectedEntityMap = findProtectedEntities();
                return Pair.pair(permissions, protectedEntityMap);
            }

            @Override
            public Collection<Role> findRolesForUser(User user) throws FindException {
                return findAllRoles();
            }

            @Override
            public Role findDefaultRoleForIdentityProvider(Goid identityProviderId) throws FindException {
                return null;
            }

            @NotNull
            @Override
            public Collection<Role> findRolesForGroup(@NotNull Group group) throws FindException {
                return null;
            }

            @Override
            public Goid saveRole(Role role) throws SaveException {
                throw new SaveException("Can't save roles in stub mode");
            }

            @Override
            public void deleteRole(Role selectedRole) throws DeleteException {
                throw new DeleteException("Can't delete roles in stub mode");
            }

            @Override
            public EntityHeaderSet<EntityHeader> findEntities(EntityType entityType) throws FindException {
                return new EntityHeaderSet<EntityHeader>(new EntityHeader(role.getId(), com.l7tech.objectmodel.EntityType.RBAC_ROLE, role.getName(), role.getDescription()));
            }

            @Override
            public Collection<SecurityZone> findAllSecurityZones() throws FindException {
                throw new FindException("not impl in stub");
            }

            @Override
            public SecurityZone findSecurityZoneByPrimaryKey(Goid goid) throws FindException {
                throw new FindException("not impl in stub");
            }

            @Override
            public Goid saveSecurityZone(SecurityZone securityZone) throws SaveException {
                throw new SaveException("not impl in stub");
            }

            @Override
            public void deleteSecurityZone(SecurityZone securityZone) throws DeleteException {
                throw new DeleteException("not impl in stub");
            }

            @Override
            public Collection<EntityHeader> findEntitiesByTypeAndSecurityZoneGoid(@NotNull EntityType type, Goid securityZoneGoid) throws FindException {
                throw new FindException("not impl in stub");
            }

            @Override
            public void setSecurityZoneForEntities(Goid securityZoneGoid, @NotNull EntityType entityType, @NotNull Collection<Serializable> entityIds) throws UpdateException {
                throw new UpdateException("not impl in stub");
            }

            @Override
            public void setSecurityZoneForEntities(Goid securityZoneGoid, @NotNull Map<EntityType, Collection<Serializable>> entityIds) throws UpdateException {
                throw new UpdateException("not impl in stub");
            }

            @Override
            public Collection<AssertionAccess> findAccessibleAssertions() {
                return Functions.map(Arrays.asList(AllAssertions.SERIALIZABLE_EVERYTHING), AssertionAccess.builderFromAssertion());
            }

            @Override
            public Goid saveAssertionAccess(AssertionAccess assertionAccess) throws UpdateException {
                throw new UpdateException("not impl in stub");
            }

            @Override
            public EntityHeader findHeader(EntityType entityType, Serializable pk) throws FindException {
                return null;
            }

            @Override
            public Entity find(@NotNull EntityHeader header) throws FindException {
                return null;
            }
        };
    }

    @Override
    public TransportAdmin getTransportAdmin() {
        return new TransportAdmin() {
            private final SsgConnector stubAdminConnection = new SsgConnector(
                    new Goid(0,2323L),
                    "stub mode admin connector",
                    8443,
                    "https",
                    true,
                    SsgConnector.Endpoint.MESSAGE_INPUT.toString(),
                    SsgConnector.CLIENT_AUTH_OPTIONAL,
                    new Goid(0,-1L),
                    null
            );

            private final SsgFirewallRule stubFirewallRule = new SsgFirewallRule(true, 1, "stubbed firewall rule");

            @Override
            public Collection<SsgConnector> findAllSsgConnectors() throws FindException {
                return Arrays.asList(stubAdminConnection);
            }

            @Override
            public SsgConnector findSsgConnectorByPrimaryKey(Goid goid) throws FindException {
                 if (stubAdminConnection.getGoid().equals(goid))
                   return stubAdminConnection;
                throw new FindException("no connector found with id " + goid);
            }

            @Override
            public Goid saveSsgConnector(SsgConnector connector) throws SaveException, UpdateException {
                throw new SaveException("Unable to save any connectors in stub mode");
            }

            @Override
            public void deleteSsgConnector(Goid oid) throws DeleteException, FindException {
                throw new DeleteException("Unable to delete any connectors in stub mode");
            }

            @Override
            public void deleteSsgActiveConnector( final Goid oid ) throws DeleteException, FindException {
                throw new FindException("Not implemented");
            }

            @Override
            public SsgActiveConnector findSsgActiveConnectorByPrimaryKey( final Goid oid ) throws FindException {
                throw new FindException("Not implemented");
            }

            @Override
            public SsgActiveConnector findSsgActiveConnectorByTypeAndName(String type, String name) throws FindException {
                throw new FindException("Not implemented");
            }

            @Override
            public Collection<SsgActiveConnector> findSsgActiveConnectorsByType( final String type ) throws FindException {
                throw new FindException("Not implemented");
            }

            @Override
            public Goid saveSsgActiveConnector( final SsgActiveConnector activeConnector ) throws SaveException, UpdateException {
                throw new SaveException("Not implemented");
            }

            @Override
            public String[] getAllProtocolVersions(boolean defaultProvderOnly) {
                return new String[0];
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
            public String[] getVisibleCipherSuiteNames() {
                return new String[0];
            }

            @Override
            public InetAddress[] getAvailableBindAddresses() {
                return null;
            }

            @Override
            public TransportDescriptor[] getModularConnectorInfo() {
                return new TransportDescriptor[0];
            }

            @Override
            public PortRanges getReservedPorts() {
                return new PortRanges(null);
            }

            @Override
            public boolean isUseIpv6() {
                return InetAddressUtil.isUseIpv6();
            }

            @Override
            public ResolutionConfiguration getResolutionConfigurationByName( final String name ) {
                return null;
            }

            @Override
            public Goid saveResolutionConfiguration( final ResolutionConfiguration configuration ) {
                return new Goid(0, 0);
            }

            @Override
            public long getXmlMaxBytes(){
                return 0L;
            }

            @Override
            public boolean isSnmpQueryEnabled() {
                return true;
            }

            @Override
            public Collection<SsgFirewallRule> findAllFirewallRules() throws FindException {
                return Arrays.asList(stubFirewallRule);
            }

            @Override
            public void deleteFirewallRule(final Goid oid) throws DeleteException, FindException, CurrentAdminConnectionException {
                throw new DeleteException("Unable to delete any firewall rules in stub mode");
            }

            @Override
            public Goid saveFirewallRule(final SsgFirewallRule firewallRule) throws SaveException, UpdateException, CurrentAdminConnectionException {
                throw new SaveException("Unable to save any firewall rules in stub mode");
            }
        };
    }

    @Override
    public EmailListenerAdmin getEmailListenerAdmin() {
        return new EmailListenerAdmin() {
            private final EmailListener emailListener = new EmailListener(new Goid(0L,2468L),
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
            public EmailListener findEmailListenerByPrimaryKey(Goid goid) throws FindException {
                if(Goid.equals(goid, new Goid(0,2468L))) {
                    return emailListener;
                } else {
                    throw new FindException("no email listener found with id " + goid);
                }
            }

            @Override
            public Collection<EmailListener> findAllEmailListeners() throws FindException {
                return Arrays.asList(emailListener);
            }

            @Override
            public Goid saveEmailListener(EmailListener emailListener) throws SaveException, UpdateException {
                throw new SaveException("Unable to save any email listeners in stub mode");
            }

            @Override
            public void deleteEmailListener(Goid goid) throws DeleteException, FindException {
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

    @NotNull
    @Override
    public HeaderBasedEntityFinder getEntityFinder() {
        throw new UnsupportedOperationException();
    }

    @Override
    public PolicyPathBuilderFactory getPolicyPathBuilderFactory() {
        return  null;
    }

    @Override
    public ResourceAdmin getResourceAdmin() {
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
    public UDDIRegistryAdmin getUDDIRegistryAdmin() {
        return null;
    }

    @Override
    public EncapsulatedAssertionAdmin getEncapsulatedAssertionAdmin() {
        return encapsulatedAssertionAdmin;
    }

    @Override
    public PolicyBackedServiceAdmin getPolicyBackedServiceAdmin() {
        return policyBackedServiceAdmin;
    }

    @Override
    public CustomKeyValueStoreAdmin getCustomKeyValueStoreAdmin() {
        return customKeyValueStoreAdmin;
    }

    @Override
    public ScheduledTaskAdmin getScheduledTaskAdmin() {
        return scheduledTaskAdmin;
    }

    @Override
    public SolutionKitAdmin getSolutionKitAdmin() {
        return null;
    }

    @Override
    public EntityNameResolver getEntityNameResolver() {
        return null;
    }

    public void setEncapsulatedAssertionAdmin(@NotNull final EncapsulatedAssertionAdmin encapsulatedAssertionAdmin) {
        this.encapsulatedAssertionAdmin = encapsulatedAssertionAdmin;
    }

    public void setPolicyBackedServiceAdmin( @NotNull PolicyBackedServiceAdmin policyBackedServiceAdmin ) {
        this.policyBackedServiceAdmin = policyBackedServiceAdmin;
    }

    public void setCustomKeyValueStoreAdmin(@NotNull final CustomKeyValueStoreAdmin customKeyValueStoreAdmin) {
        this.customKeyValueStoreAdmin = customKeyValueStoreAdmin;
    }

    public void setScheduledTaskAdmin (@NotNull final ScheduledTaskAdmin scheduledTaskAdmin) {
        this.scheduledTaskAdmin = scheduledTaskAdmin;
    }

    @Override
    public WorkQueueManagerAdmin getWorkQueueManagerAdmin() {
        return null;
    }

    @Override
    public <T> T getExtensionInterface(Class<T> interfaceClass, String instanceIdentifier) {
        //noinspection unchecked
        return (T) Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class[]{interfaceClass}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                throw new NoSuchMethodException("No extension interfaces in stub");
            }
        });
    }

    @Override
    public <T> Option<T> getAdminInterface( final Class<T> interfaceClass ) {
        return none();
    }

    @Override
    public EmailAdmin getEmailAdmin() {
        return new EmailAdmin() {
            @Override
            public void testSendEmail(EmailAlertAssertion eaa) throws EmailTestException {
            }

            @Override
            public long getXmlMaxBytes() {
                return 0L;
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
    private TrustedCertAdmin trustedCertAdmin = new TrustedCertAdminStub();
    private EncapsulatedAssertionAdmin encapsulatedAssertionAdmin;
    private PolicyBackedServiceAdmin policyBackedServiceAdmin;
    private CustomKeyValueStoreAdmin customKeyValueStoreAdmin;
    private ScheduledTaskAdmin scheduledTaskAdmin;
    private ServiceAdmin serviceManager;
    private PolicyAdmin policyAdmin;
    private FolderAdmin folderAdmin;
    private boolean adminContextPresent;
}
