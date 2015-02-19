package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.gateway.api.EmailListenerMO;
import com.l7tech.gateway.common.transport.email.EmailListener;
import com.l7tech.gateway.common.transport.email.EmailServerType;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.security.rbac.SecurityFilter;
import com.l7tech.server.security.rbac.SecurityZoneManager;
import com.l7tech.server.transport.email.EmailListenerManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.transaction.PlatformTransactionManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author alee, 1/23/2015
 */
@RunWith(MockitoJUnitRunner.class)
public class EmailListenerResourceFactoryTest {
    @Mock
    private RbacServices rbacServices;
    @Mock
    private SecurityFilter securityFilter;
    @Mock
    private PlatformTransactionManager transactionManager;
    @Mock
    private EmailListenerManager emailListenerManager;
    @Mock
    private ServiceResourceFactory serviceResourceFactory;
    @Mock
    private SecurityZoneManager securityZoneManager;
    private EmailListenerResourceFactory factory;
    private EmailListener listener;

    @Before
    public void setup() {
        factory = new EmailListenerResourceFactory(rbacServices, securityFilter, transactionManager, emailListenerManager, serviceResourceFactory, securityZoneManager);
        listener = new EmailListener();
        listener.setServerType(EmailServerType.POP3);
    }

    @Test
    public void asResourceNullPassword() {
        listener.setPassword(null);
        final EmailListenerMO mo = factory.asResource(listener);
        assertNull(mo.getPassword());
    }

    @Test
    public void asResourceIgnoresPlaintextPassword() {
        listener.setPassword("plaintext");
        final EmailListenerMO mo = factory.asResource(listener);
        assertNull(mo.getPassword());
    }

    @Test
    public void asResourceIncludesSecurePasswordReference() {
        final String securePassReference = "${secpass.mypass.plaintext}";
        listener.setPassword(securePassReference);
        final EmailListenerMO mo = factory.asResource(listener);
        assertEquals(securePassReference, mo.getPassword());
    }
}
