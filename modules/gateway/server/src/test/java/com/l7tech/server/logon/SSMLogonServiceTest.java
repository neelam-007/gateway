package com.l7tech.server.logon;

import com.l7tech.identity.LogonInfo;
import com.l7tech.identity.UserBean;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.util.Config;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.transaction.PlatformTransactionManager;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SSMLogonServiceTest {

    // mocked objects
    @Mock private PlatformTransactionManager platformTransactionManager;
    @Mock private LogonInfoManager logonInfoManager;
    @Mock private Config config;
    @Mock private RoleManager roleManager;
    @Mock private IdentityProviderFactory identityProviderFactory;
    // real objects
    private UserBean user;
    private String login;
    private SSMLogonService ssmLogonService;
    private LogonInfo logonInfo;

    @Before
    public void setUp() throws Exception {
        login = "test";
        user = new UserBean(Goid.DEFAULT_GOID, login);
        ssmLogonService = new SSMLogonService(platformTransactionManager, logonInfoManager, config, roleManager, identityProviderFactory);
        logonInfo = new LogonInfo(Goid.DEFAULT_GOID, login);
    }

    @Test
    public void doResetLockedUser() {
        try {
            logonInfo.setState(LogonInfo.State.EXCEED_ATTEMPT);
            logonInfo.setFailCount(5);
            when(ssmLogonService.getLogonManager().findByCompositeKey(user.getProviderId(), user.getLogin(), true)).thenReturn(logonInfo);
            final Long now = System.currentTimeMillis();
            // test that the state has reset, and that the fail count has reset
            ssmLogonService.doResetLockedUser(user, now);
            assertEquals(LogonInfo.State.ACTIVE, logonInfo.getState());
            assertEquals(logonInfo.getFailCount(), 0);

        } catch (Exception e) {
            fail("Should not throw an exception");
        }
    }
}