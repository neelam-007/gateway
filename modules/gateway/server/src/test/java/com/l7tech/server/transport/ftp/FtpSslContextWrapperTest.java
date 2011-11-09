package com.l7tech.server.transport.ftp;

import com.l7tech.test.util.TestUtils;
import org.junit.Test;

import javax.net.ssl.SSLContextSpi;

/**
 *
 */
public class FtpSslContextWrapperTest {

    @Test
    public void testDelegation() {
        TestUtils.assertOverridesAllMethods( SSLContextSpi.class, FtpSslContextWrapper.class );
    }
}
