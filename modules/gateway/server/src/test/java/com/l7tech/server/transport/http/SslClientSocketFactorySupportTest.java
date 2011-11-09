package com.l7tech.server.transport.http;

import com.l7tech.test.util.TestUtils;
import org.junit.Test;

import javax.net.ssl.SSLSocketFactory;

/**
 *
 */
public class SslClientSocketFactorySupportTest {

    @Test
    public void testDelegation() {
        TestUtils.assertOverridesAllMethods( SSLSocketFactory.class, SslClientSocketFactorySupport.class );
    }
}
