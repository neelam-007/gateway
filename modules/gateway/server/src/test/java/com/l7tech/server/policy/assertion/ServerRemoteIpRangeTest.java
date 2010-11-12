package com.l7tech.server.policy.assertion;

import com.l7tech.util.InetAddressUtil;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.policy.assertion.RemoteIpRange;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.net.InetAddress;

/**
 * Test logic of ServerRemoteIpRange.
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Feb 23, 2004<br/>
 * $Id$<br/>
 *
 */
public class ServerRemoteIpRangeTest extends TestCase {

    public static void main(String[] args) throws Throwable {
        junit.textui.TestRunner.run(suite());
    }

    public static Test suite() {
        return new TestSuite(ServerRemoteIpRangeTest.class);
    }

    public void testInclusions() {
        RemoteIpRange rule = new RemoteIpRange("192.168.11.0", 24, true);
        ServerRemoteIpRange testee = new ServerRemoteIpRange(rule, ApplicationContexts.getTestApplicationContext());

        InetAddress addToTest = InetAddressUtil.getAddress("192.168.11.0");
        boolean res = testee.assertAddress(addToTest);
        assertTrue(addToTest + " should pass", res);

        addToTest = InetAddressUtil.getAddress("192.168.11.5");
        res = testee.assertAddress(addToTest);
        assertTrue(addToTest + " should pass", res);

        addToTest = InetAddressUtil.getAddress("192.168.11.99");
        res = testee.assertAddress(addToTest);
        assertTrue(addToTest + " should pass", res);

        addToTest = InetAddressUtil.getAddress("192.168.11.255");
        res = testee.assertAddress(addToTest);
        assertTrue(addToTest + " should pass", res);

        addToTest = InetAddressUtil.getAddress("192.168.1.0");
        res = testee.assertAddress(addToTest);
        assertFalse(addToTest + " should NOT pass", res);

        addToTest = InetAddressUtil.getAddress("192.178.11.22");
        res = testee.assertAddress(addToTest);
        assertFalse(addToTest + " should NOT pass", res);

        addToTest = InetAddressUtil.getAddress("10.168.1.2");
        res = testee.assertAddress(addToTest);
        assertFalse(addToTest + " should NOT pass", res);
    }

    public void testExclusions() {
        RemoteIpRange rule = new RemoteIpRange("10.0.0.0", 24, false);
        ServerRemoteIpRange testee = new ServerRemoteIpRange(rule, ApplicationContexts.getTestApplicationContext());

        InetAddress addToTest = InetAddressUtil.getAddress("10.1.0.1");
        boolean res = testee.assertAddress(addToTest);
        assertTrue(addToTest + " should pass", res);

        addToTest = InetAddressUtil.getAddress("52.1.97.1");
        res = testee.assertAddress(addToTest);
        assertTrue(addToTest + " should pass", res);

        addToTest = InetAddressUtil.getAddress("10.0.0.0");
        res = testee.assertAddress(addToTest);
        assertFalse(addToTest + " should NOT pass", res);

        addToTest = InetAddressUtil.getAddress("10.0.0.255");
        res = testee.assertAddress(addToTest);
        assertFalse(addToTest + " should NOT pass", res);

        addToTest = InetAddressUtil.getAddress("10.0.0.77");
        res = testee.assertAddress(addToTest);
        assertFalse(addToTest + " should NOT pass", res);
    }
}
