package com.l7tech.server.admin;

import com.l7tech.common.password.PasswordHasher;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.ApplicationContextTest;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.cluster.ClusterMaster;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.identity.internal.InternalIdentityProvider;
import com.l7tech.server.identity.internal.InternalUserManager;
import com.l7tech.server.identity.internal.InternalUserManagerImpl;
import com.l7tech.server.identity.internal.InternalUserPasswordManager;
import com.l7tech.server.logon.LogonInfoManager;
import com.l7tech.server.logon.LogonService;
import com.l7tech.server.logon.SSMLogonService;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.server.util.ManagedTimer;
import com.l7tech.util.Pair;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.*;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AdminSessionManagerTest {

    private ServerConfig serverConfig = ServerConfig.getInstance();

    @Mock private LogonService logonService;
    @Mock private ClusterMaster clusterMaster;
    @Mock private PlatformTransactionManager transactionManager;
    @Mock private LogonInfoManager logonInfoManager;
    @Mock private RoleManager roleManager;
    @Mock private IdentityProviderFactory identityProviderFactory;
    @Mock private InternalIdentityProvider identityProvider;
    @Mock private InternalUserManager userManager;
    @Mock private ClientCertManager clientCertManager;
    @Mock private PasswordHasher passwordHasher;
    @Mock private InternalUserPasswordManager userPasswordManager;

    @Test
    public void testSSMLogonServiceViaExplicitRoleAssignmentsForValidUser() throws FindException {
        ApplicationContextTest context = new ApplicationContextTest();
        SSMLogonService ssmLogonService = new SSMLogonService(
                transactionManager, logonInfoManager, serverConfig, roleManager, identityProviderFactory);
        Collection<Pair<Goid, String>> pairs = new ArrayList<>();
        pairs.add(new Pair<Goid, String>(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID, "user"));

        when(roleManager.getExplicitRoleAssignments()).thenReturn(pairs);
        when(identityProviderFactory.getProvider(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID)).thenReturn(identityProvider);
        when(identityProvider.getUserManager()).thenReturn(userManager);
        when(userManager.findByPrimaryKey(anyString())).thenReturn(new InternalUser("user"));

        List<String> logs = new ArrayList<>();
        Logger logger = Logger.getLogger(SSMLogonService.class.getName());
        logger.addHandler(createLogHandler(logs));

        ssmLogonService.checkLogonInfos();
        assertFalse(logs.contains("Internal user not found: user"));
    }

    @Test
    public void testSSMLogonServiceViaExplicitRoleAssignmentsForMissingUser() throws FindException {
        ApplicationContextTest context = new ApplicationContextTest();
        SSMLogonService ssmLogonService = new SSMLogonService(
                transactionManager, logonInfoManager, serverConfig, roleManager, identityProviderFactory);
        Collection<Pair<Goid, String>> pairs = new ArrayList<>();
        pairs.add(new Pair<Goid, String>(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID, "null_user"));

        when(roleManager.getExplicitRoleAssignments()).thenReturn(pairs);
        when(identityProviderFactory.getProvider(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID)).thenReturn(identityProvider);
        when(identityProvider.getUserManager()).thenReturn(userManager);
        when(userManager.findByPrimaryKey(anyString())).thenReturn(null);

        List<String> logs = new ArrayList<>();
        Logger logger = Logger.getLogger(SSMLogonService.class.getName());
        logger.addHandler(createLogHandler(logs));

        ssmLogonService.checkLogonInfos();
        assertTrue(logs.contains("Internal user not found: null_user"));
    }

    private Handler createLogHandler(List<String> logs) {
        return new Handler() {
            @Override
            public void publish(LogRecord record) {
                logs.add(record.getMessage());
            }

            @Override
            public void flush() {

            }

            @Override
            public void close() throws SecurityException {

            }
        };
    }
}
