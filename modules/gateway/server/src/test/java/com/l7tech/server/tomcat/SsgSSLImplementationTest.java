package com.l7tech.server.tomcat;

import com.l7tech.test.util.TestUtils;
import org.apache.tomcat.util.net.SSLImplementation;
import org.junit.Test;

/**
 *
 */
public class SsgSSLImplementationTest {

    @Test
    public void testDelegation() {
        TestUtils.assertOverridesAllMethods( SSLImplementation.class, SsgSSLImplementation.class );
    }
}
