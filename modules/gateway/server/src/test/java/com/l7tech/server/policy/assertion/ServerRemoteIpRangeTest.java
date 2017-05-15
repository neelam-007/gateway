package com.l7tech.server.policy.assertion;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.message.Message;
import com.l7tech.message.TcpKnob;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.RemoteIpRange;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.test.BugNumber;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.InetAddressUtil;
import org.junit.Before;
import org.junit.Test;

import java.net.InetAddress;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test logic of ServerRemoteIpRange.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Feb 23, 2004<br/>
 */
public class ServerRemoteIpRangeTest {

    private RemoteIpRange assertion;
    private ServerRemoteIpRange serverAssertion;
    private AssertionStatus status;

    private TestAudit testAudit;
    private PolicyEnforcementContext peCtx;

    @Before
    public void setUp() throws Exception {
        peCtx = makeContext("<myrequest/>", "<myresponse/>");
        testAudit = new TestAudit();
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

    @Test
    public void testIpRangeWithDefinedVariables() throws Exception {
        assertion = new RemoteIpRange();
        assertion.setAddressRange("192.168.${partialIp}.12", "${prefix}");
        assertion.setIpSourceContextVariable("sourceIp");
        peCtx.setVariable("partialIp", "11");
        peCtx.setVariable("prefix", "30");

        peCtx.setVariable("sourceIp", "192.168.11.12");
        serverAssertion = createServer(assertion);
        status = serverAssertion.checkRequest(peCtx);
        assertEquals(AssertionStatus.NONE, status);

        peCtx.setVariable("sourceIp", "192.168.11.13");
        serverAssertion = createServer(assertion);
        status = serverAssertion.checkRequest(peCtx);
        assertEquals(AssertionStatus.NONE, status);

        peCtx.setVariable("sourceIp", "192.168.11.15");
        serverAssertion = createServer(assertion);
        status = serverAssertion.checkRequest(peCtx);
        assertEquals(AssertionStatus.NONE, status);
    }

    @Test
    public void testIpRangeWithDefinedVariablesForRejection() throws Exception {
        assertion = new RemoteIpRange();
        assertion.setAddressRange("192.168.${partialIp}.12", "${prefix}");
        assertion.setIpSourceContextVariable("sourceIp");

        peCtx.setVariable("partialIp", "11");
        peCtx.setVariable("prefix", "30");
        peCtx.setVariable("sourceIp", "192.168.11.16");

        serverAssertion = createServer(assertion);
        status = serverAssertion.checkRequest(peCtx);
        assertEquals(AssertionStatus.FALSIFIED, status);
        assertEquals(1, testAudit.getAuditCount());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.IP_REJECTED));
    }

    @Test
    public void testDefaultServerAssertionState() throws Exception {
        TcpKnob tcpKnob = mock(TcpKnob.class);
        when(tcpKnob.getRemoteAddress()).thenReturn("192.168.1.12");
        peCtx.getRequest().attachKnob(tcpKnob, TcpKnob.class);

        serverAssertion = createServer(new RemoteIpRange());
        status = serverAssertion.checkRequest(peCtx);
        assertEquals(AssertionStatus.NONE, status);
    }

    @Test
    public void testInvalidIpRange() throws Exception {
        assertion = new RemoteIpRange();
        assertion.setAddressRange("192.168.${partialIp}.12", "${prefix}");
        assertion.setIpSourceContextVariable("sourceIp");

        peCtx.setVariable("partialIp", "11");
        peCtx.setVariable("prefix", "3#0");
        peCtx.setVariable("sourceIp", "192.168.11.16");

        serverAssertion = createServer(assertion);
        status = serverAssertion.checkRequest(peCtx);
        assertEquals(AssertionStatus.FAILED, status);
        assertEquals(1, testAudit.getAuditCount());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.IP_INVALID_RANGE));
    }

    @Test
    public void testInvalidIpRangeDueToNoSuchVariable() throws Exception {
        assertion = new RemoteIpRange();
        assertion.setAddressRange("192.168.${partialIp}.12", "${prefix}");
        assertion.setIpSourceContextVariable("sourceIp");

        peCtx.setVariable("partialIp", "11");
        peCtx.setVariable("sourceIp", "192.168.11.16");

        serverAssertion = createServer(assertion);
        status = serverAssertion.checkRequest(peCtx);
        assertEquals(AssertionStatus.SERVER_ERROR, status);
        assertEquals(1, testAudit.getAuditCount());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.NO_SUCH_VARIABLE));
    }

    private PolicyEnforcementContext makeContext(String req, String res) {
        Message request = new Message();
        request.initialize(XmlUtil.stringAsDocument(req));
        Message response = new Message();
        response.initialize(XmlUtil.stringAsDocument(res));
        return PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
    }

    private ServerRemoteIpRange createServer(RemoteIpRange assertion) {
        ServerRemoteIpRange serverAssertion = new ServerRemoteIpRange(assertion);

        ApplicationContexts.inject(serverAssertion, CollectionUtils.<String, Object>mapBuilder()
                .put("auditFactory", testAudit.factory())
                .unmodifiableMap()
        );

        return serverAssertion;
    }

}
