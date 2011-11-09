package com.l7tech.common.io;

import com.l7tech.test.util.TestUtils;
import org.junit.Test;

import javax.net.ssl.SSLSocket;

/**
 *
 */
public class SSLSocketWrapperTest {

    @Test
    public void testDelegation() {
        TestUtils.assertOverridesAllMethods( SSLSocket.class, SSLSocketWrapper.class );
    }
}
