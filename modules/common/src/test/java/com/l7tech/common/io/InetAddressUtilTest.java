package com.l7tech.common.io;

import static junit.framework.Assert.assertEquals;
import org.junit.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Unit tests for {@link InetAddressUtil}.
 */
public class InetAddressUtilTest {

    @Test
    public void testToLong() throws UnknownHostException {
        assertEquals(3872534927L, InetAddressUtil.toLong(InetAddress.getByName("230.210.49.143")));
        assertEquals(2130706433L, InetAddressUtil.toLong(InetAddress.getByName("127.0.0.1")));
    }
}
