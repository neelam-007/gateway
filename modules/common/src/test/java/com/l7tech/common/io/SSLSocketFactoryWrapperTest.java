package com.l7tech.common.io;

import com.l7tech.test.util.TestUtils;
import org.junit.Test;

import javax.net.ssl.SSLSocketFactory;

/**
 *
 */
public class SSLSocketFactoryWrapperTest {

    @Test
    public void testDelegation() {
        TestUtils.assertOverridesAllMethods( SSLSocketFactory.class, SSLSocketFactoryWrapper.class );
    }
}
