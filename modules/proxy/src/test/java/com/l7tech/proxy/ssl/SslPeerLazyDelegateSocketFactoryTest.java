package com.l7tech.proxy.ssl;

import com.l7tech.test.util.TestUtils;
import org.junit.Test;

import javax.net.ssl.SSLSocketFactory;

/**
 *
 */
public class SslPeerLazyDelegateSocketFactoryTest {

    @Test
    public void testDelegation() {
        TestUtils.assertOverridesAllMethods( SSLSocketFactory.class, SslPeerLazyDelegateSocketFactory.class );
    }
}
