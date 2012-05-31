package com.l7tech.server.policy.assertion;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.RemoteIpRange;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.test.BugNumber;
import com.l7tech.util.InetAddressUtil;
import org.junit.Before;
import org.junit.Test;

import java.net.InetAddress;

import static org.junit.Assert.*;

/**
 * Test logic of ServerRemoteIpRange.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Feb 23, 2004<br/>
 */
public class ServerRemoteIpRangeTest {

    private PolicyEnforcementContext peCtx;

    @Before
    public void setUp() throws Exception {
        peCtx = makeContext("<myrequest/>", "<myresponse/>");
    }

    public void testInclusions() throws Exception {
        RemoteIpRange rule = new RemoteIpRange("192.168.11.0", 24, true);
        ServerRemoteIpRange testee = new ServerRemoteIpRange(rule);

        InetAddress addToTest = InetAddressUtil.getAddress("192.168.11.0");
        boolean res = testee.assertAddress(addToTest, peCtx);
        assertTrue(addToTest + " should pass", res);

        addToTest = InetAddressUtil.getAddress("192.168.11.5");
        res = testee.assertAddress(addToTest, peCtx);
        assertTrue(addToTest + " should pass", res);

        addToTest = InetAddressUtil.getAddress("192.168.11.99");
        res = testee.assertAddress(addToTest, peCtx);
        assertTrue(addToTest + " should pass", res);

        addToTest = InetAddressUtil.getAddress("192.168.11.255");
        res = testee.assertAddress(addToTest, peCtx);
        assertTrue(addToTest + " should pass", res);

        addToTest = InetAddressUtil.getAddress("192.168.1.0");
        res = testee.assertAddress(addToTest, peCtx);
        assertFalse(addToTest + " should NOT pass", res);

        addToTest = InetAddressUtil.getAddress("192.178.11.22");
        res = testee.assertAddress(addToTest, peCtx);
        assertFalse(addToTest + " should NOT pass", res);

        addToTest = InetAddressUtil.getAddress("10.168.1.2");
        res = testee.assertAddress(addToTest, peCtx);
        assertFalse(addToTest + " should NOT pass", res);

        testee = new ServerRemoteIpRange(new RemoteIpRange("2222::", 64, true));

        addToTest = InetAddressUtil.getAddress("2222::215");
        res = testee.assertAddress(addToTest, peCtx);
        assertTrue(addToTest + " should pass", res);

        addToTest = InetAddressUtil.getAddress("2221::215");
        res = testee.assertAddress(addToTest, peCtx);
        assertFalse(addToTest + " should NOT pass", res);
    }

    @Test
    public void testExclusions() throws Exception {
        RemoteIpRange rule = new RemoteIpRange("10.0.0.0", 24, false);
        ServerRemoteIpRange testee = new ServerRemoteIpRange(rule);

        InetAddress addToTest = InetAddressUtil.getAddress("10.1.0.1");
        boolean res = testee.assertAddress(addToTest, peCtx);
        assertTrue(addToTest + " should pass", res);

        addToTest = InetAddressUtil.getAddress("52.1.97.1");
        res = testee.assertAddress(addToTest, peCtx);
        assertTrue(addToTest + " should pass", res);

        addToTest = InetAddressUtil.getAddress("10.0.0.0");
        res = testee.assertAddress(addToTest, peCtx);
        assertFalse(addToTest + " should NOT pass", res);

        addToTest = InetAddressUtil.getAddress("10.0.0.255");
        res = testee.assertAddress(addToTest, peCtx);
        assertFalse(addToTest + " should NOT pass", res);

        addToTest = InetAddressUtil.getAddress("10.0.0.77");
        res = testee.assertAddress(addToTest, peCtx);
        assertFalse(addToTest + " should NOT pass", res);

        testee = new ServerRemoteIpRange(new RemoteIpRange("2222::", 64, false));

        addToTest = InetAddressUtil.getAddress("2222::215");
        res = testee.assertAddress(addToTest, peCtx);
        assertFalse(addToTest + " should NOT pass", res);

        addToTest = InetAddressUtil.getAddress("2221::215");
        res = testee.assertAddress(addToTest, peCtx);
        assertTrue(addToTest + " should pass", res);
    }

    @Test
    @BugNumber(11538)
    public void testWhenVariablesAreUsed() throws Exception {
        //test per IP Address quadrant {XXX}.{XXX}.{XXX}.{XXX} or {XXX}:{XXX}:{XXX}
        peCtx.setVariable("ipVar1", "0");

        RemoteIpRange rule = new RemoteIpRange("10.0.0.${ipVar1}", 32, false);
        ServerRemoteIpRange testee = new ServerRemoteIpRange(rule);

        InetAddress addToTest = InetAddressUtil.getAddress("10.1.0.1");
        boolean res = testee.assertAddress(addToTest, peCtx);
        assertTrue(addToTest + " should pass", res);

        peCtx.setVariable("ipVar2", "2222");

        testee = new ServerRemoteIpRange(new RemoteIpRange("${ipVar2}::", 64, false));

        addToTest = InetAddressUtil.getAddress("2222::215");
        res = testee.assertAddress(addToTest, peCtx);
        assertFalse(addToTest + " should NOT pass", res);

        addToTest = InetAddressUtil.getAddress("2221::215");
        res = testee.assertAddress(addToTest, peCtx);
        assertTrue(addToTest + " should pass", res);

        //test full IP address as a context variable
        peCtx.setVariable("ipVar3", "10.0.0.0");
        addToTest = InetAddressUtil.getAddress("10.1.0.10");
        res = testee.assertAddress(addToTest, peCtx);
        assertTrue(addToTest + " should pass", res);

        //test undefined context variable
        rule = new RemoteIpRange("10.0.0.${ipVarXXX}", 32, false);
        testee = new ServerRemoteIpRange(rule);
        try {
            addToTest = InetAddressUtil.getAddress("10.1.0.1");
            res = testee.assertAddress(addToTest, peCtx);
            fail("Should have not reached this code. Test above should have thrown an exception should fail");
        } catch (NoSuchVariableException e) {
            //we are expecting this :)
        }
    }

    private PolicyEnforcementContext makeContext(String req, String res) {
        Message request = new Message();
        request.initialize(XmlUtil.stringAsDocument(req));
        Message response = new Message();
        response.initialize(XmlUtil.stringAsDocument(res));
        return PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
    }
}
