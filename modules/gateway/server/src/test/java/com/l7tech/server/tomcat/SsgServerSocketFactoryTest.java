package com.l7tech.server.tomcat;

import com.l7tech.test.util.TestUtils;
import org.apache.tomcat.util.net.ServerSocketFactory;
import org.junit.Test;

/**
 *
 */
public class SsgServerSocketFactoryTest {

    @Test
    public void testDelegation() {
        TestUtils.assertOverridesAllMethods( ServerSocketFactory.class, SsgServerSocketFactory.class );
    }
}
