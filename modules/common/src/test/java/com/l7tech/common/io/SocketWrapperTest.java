package com.l7tech.common.io;

import com.l7tech.test.util.TestUtils;
import org.junit.Test;

import java.net.Socket;

/**
 *
 */
public class SocketWrapperTest {

    @Test
    public void testDelegation() {
        TestUtils.assertOverridesAllMethods( Socket.class, SocketWrapper.class );
    }
}
