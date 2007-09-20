package com.l7tech.console.util.registry;

import com.l7tech.cluster.ClusterStatusAdmin;
import com.l7tech.cluster.ClusterStatusAdminStub;
import com.l7tech.common.TestLicenseManager;
import com.l7tech.common.VersionException;
import com.l7tech.common.audit.AuditAdmin;
import com.l7tech.common.audit.AuditAdminStub;
import com.l7tech.common.security.TrustedCertAdmin;
import com.l7tech.common.security.kerberos.KerberosAdmin;
import com.l7tech.common.security.rbac.EntityType;
import com.l7tech.common.security.rbac.*;
import com.l7tech.common.transport.ftp.FtpAdmin;
import com.l7tech.common.transport.jms.JmsAdmin;
import com.l7tech.common.transport.SsgConnector;
import com.l7tech.common.transport.TransportAdmin;
import com.l7tech.common.xml.schema.SchemaAdmin;
import com.l7tech.console.security.SecurityProvider;
import com.l7tech.console.util.ConsoleLicenseManager;
import com.l7tech.console.util.Registry;
import com.l7tech.identity.*;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.assertion.ext.CustomAssertionsRegistrar;
import com.l7tech.policy.assertion.ext.CustomAssertionsRegistrarStub;
import com.l7tech.policy.validator.DefaultPolicyValidator;
import com.l7tech.service.*;

import javax.security.auth.login.LoginException;
import java.net.PasswordAuthentication;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Collection;


/**
 * Test, stub registry.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class RegistryStub extends Registry {
    private Role role;

    /**
     * default constructor
     */
    public RegistryStub() {
        serviceAdmin.setPolicyValidator(new DefaultPolicyValidator());
        serviceAdmin.setServiceManager(new ServiceManagerStub());
    }

    public boolean isAdminContextPresent() {
        return serviceAdmin != null;
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
        return serviceAdmin;
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
        return customAssertionsRegistrar;
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

    public RbacAdmin getRbacAdmin() {
        return new RbacAdmin() {
            private final Role role = new Role();
            {
                role.setName("Stub role");
                role.setOid(-777);
                role.setDescription("Fake role for testing");
            }

            public Collection<Role> findAllRoles() throws FindException, RemoteException {
                return Arrays.asList(role);
            }

            public Role findRoleByPrimaryKey(long oid) throws FindException, RemoteException {
                if (role.getOid() == oid) return role;
                return null;
            }

            public Collection<Permission> findCurrentUserPermissions() throws FindException, RemoteException {
                return Arrays.asList(
                        new Permission(role, OperationType.CREATE, EntityType.ANY),
                        new Permission(role, OperationType.READ, EntityType.ANY),
                        new Permission(role, OperationType.UPDATE, EntityType.ANY),
                        new Permission(role, OperationType.DELETE, EntityType.ANY),
                        new Permission(role, OperationType.OTHER, EntityType.ANY)
                );
            }

            public Collection<Role> findRolesForUser(User user) throws FindException, RemoteException {
                return findAllRoles();
            }

            public long saveRole(Role role) throws SaveException, RemoteException {
                throw new SaveException("Can't save roles in stub mode");
            }

            public void deleteRole(Role selectedRole) throws DeleteException, RemoteException {
                throw new DeleteException("Can't delete roles in stub mode");
            }

            public EntityHeader[] findEntities(Class<? extends Entity> entityClass) throws FindException, RemoteException {
                return new EntityHeader[] {new EntityHeader(role.getId(), com.l7tech.objectmodel.EntityType.RBAC_ROLE, role.getName(), role.getDescription())};
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
                    SsgConnector.CLIENT_AUTH_OPTIONAL,
                    -1L,
                    null
            );

            public Collection<SsgConnector> findAllSsgConnectors() throws RemoteException, FindException {
                return Arrays.asList(stubAdminConnection);
            }

            public SsgConnector findSsgConnectorByPrimaryKey(long oid) throws RemoteException, FindException {
                if (stubAdminConnection.getOid() == oid)
                    return stubAdminConnection;
                throw new FindException("no connector found with id " + oid);
            }

            public long saveSsgConnector(SsgConnector connector) throws RemoteException, SaveException, UpdateException {
                throw new SaveException("Unable to save any connectors in stub mode");
            }

            public void deleteSsgConnector(long oid) throws RemoteException, DeleteException, FindException {
                throw new DeleteException("Unable to delete any connectors in stub mode");
            }
        };
    }

    public SecurityProvider getSecurityProvider() {
        return new SecurityProvider() {
            public void login(PasswordAuthentication creds, String host, boolean validateHost) throws LoginException, VersionException, RemoteException {
            }

            public void login(String sessionId, String host) throws LoginException, VersionException, RemoteException {
            }

            public void changePassword(PasswordAuthentication auth, PasswordAuthentication newAuth) throws LoginException, RemoteException {
            }

            public void logoff() {                
            }
        };
    }

    public SchemaAdmin getSchemaAdmin() {
        return null;
    }

    public ConsoleLicenseManager getLicenseManager() {
        return licenseManager;
    }

    StubDataStore dataStore = StubDataStore.defaultStore();

    private IdentityAdmin identityAdmin = new IdentityAdminStub();
    private ServiceAdminStub serviceAdmin = new ServiceAdminStub();
    private JmsAdmin jmsAdmin = new JmsAdminStub();
    private FtpAdmin ftpAdmin = new FtpAdminStub();
    private CustomAssertionsRegistrar customAssertionsRegistrar = new CustomAssertionsRegistrarStub();
    private AuditAdmin auditAdmin = new AuditAdminStub();
    private ClusterStatusAdmin clusterStatusAdmin = new ClusterStatusAdminStub();
    private TrustedCertAdmin trustedCertAdmin;
    private ConsoleLicenseManager licenseManager = new TestLicenseManager();
}
