package com.l7tech.server.transport.email;

import com.l7tech.server.transport.email.EmailUtils.StartTlsSocketFactory;
import com.l7tech.test.util.TestUtils;
import org.junit.Test;

import javax.net.ssl.SSLSocketFactory;

/**
 *
 */
public class EmailUtilsTest {

    @Test
    public void testDelegation() {
        TestUtils.assertOverridesAllMethods( SSLSocketFactory.class, StartTlsSocketFactory.class );
    }
}
