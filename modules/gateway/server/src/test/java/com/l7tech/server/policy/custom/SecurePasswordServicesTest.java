package com.l7tech.server.policy.custom;

import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.ext.password.SecurePasswordServices;
import com.l7tech.policy.assertion.ext.ServiceException;
import com.l7tech.server.policy.SecurePasswordServicesImpl;
import com.l7tech.server.security.password.SecurePasswordManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SecurePasswordServicesTest {

    @Mock
    SecurePasswordManager securePasswordManagerMock;

    @Test
    public void testDecryptPassword() throws Exception {
        when(securePasswordManagerMock.findByPrimaryKey(any(Goid.class))).thenReturn(new SecurePassword());
        when(securePasswordManagerMock.decryptPassword(anyString())).thenReturn("password".toCharArray());

        SecurePasswordServices securePasswordServices = new SecurePasswordServicesImpl(securePasswordManagerMock);
        Goid passwordGoid = new Goid(0,1000L);
        String decryptedPassword = securePasswordServices.decryptPassword(passwordGoid.toString());
        assertNotNull(decryptedPassword);
        assertEquals("password", decryptedPassword);
    }

    @Test(expected = ServiceException.class)
    public void testPasswordNotFound() throws Exception {
        when(securePasswordManagerMock.findByPrimaryKey(any(Goid.class))).thenReturn(null);

        SecurePasswordServices securePasswordServices = new SecurePasswordServicesImpl(securePasswordManagerMock);
        Goid passwordGoid = SecurePassword.DEFAULT_GOID;
        securePasswordServices.decryptPassword(passwordGoid.toString());
    }
}