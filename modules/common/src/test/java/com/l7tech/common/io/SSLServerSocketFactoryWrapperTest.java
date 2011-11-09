package com.l7tech.common.io;

import com.l7tech.test.util.TestUtils;
import org.junit.Test;

import javax.net.ssl.SSLServerSocketFactory;

/**
 *
 */
public class SSLServerSocketFactoryWrapperTest {

    @Test
    public void testDelegation() {
        TestUtils.assertOverridesAllMethods( SSLServerSocketFactory.class, SSLServerSocketFactoryWrapper.class );
    }
}
