package com.l7tech.external.assertions.snmptrap.server;

import com.l7tech.external.assertions.snmptrap.SnmpTrapAssertion;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.snmp4j.MessageDispatcher;
import org.snmp4j.PDU;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.security.SecurityModel;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;

import java.net.InetAddress;

import static org.junit.Assert.*;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ServerSnmpTrapAssertionTest {
    private static final String COMMUNITY = "l7";
    private static final String HOST = "test.l7tech.com";
    private static final String OID = "1";
    private static final String OID_PREFIX = "1.3.6.1.4.1.17304.7.3.";
    private static final String FULL_OID = OID_PREFIX + OID;
    private static final int PORT = 162;
    private static final String MESSAGE = "my snmp message";
    private SnmpTrapAssertion assertion;
    private ServerSnmpTrapAssertion serverAssertion;
    private PolicyEnforcementContext context;
    @Mock
    private MessageDispatcher dispatcher;
    @Mock
    private ServerSnmpTrapAssertion.InetAddressWrapper inetAddressWrapper;
    @Mock
    private ServerSnmpTrapAssertion.UptimeMonitorWrapper uptimeMonitorWrapper;
    @Mock
    private InetAddress address;

    @Before
    public void setup() {
        assertion = new SnmpTrapAssertion();
        assertion.setCommunity(COMMUNITY);
        assertion.setOid(OID);
        assertion.setTargetHostname(HOST);
        assertion.setTargetPort(PORT);
        assertion.setErrorMessage(MESSAGE);
        context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
    }

    @Test
    public void noContextVariables() throws Exception {
        initServerAssertion();

        when(inetAddressWrapper.getByName(HOST)).thenReturn(address);

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));

        verify(uptimeMonitorWrapper).getLastUptime();
        verify(inetAddressWrapper).getByName(HOST);
        verify(dispatcher).sendPdu(udpAddressWithPort(PORT), eq(SnmpConstants.version2c), eq(SecurityModel.SECURITY_MODEL_SNMPv2c),
                eq(COMMUNITY.getBytes()),
                eq(SecurityLevel.NOAUTH_NOPRIV),
                pduWithOidAndMessage(FULL_OID, MESSAGE),
                eq(false));
    }

    @Test
    public void errorMessageContextVariable() throws Exception {
        assertion.setErrorMessage("${errMsg}");
        initServerAssertion();
        context.setVariable("errMsg", "Custom Error Message");

        when(inetAddressWrapper.getByName(HOST)).thenReturn(address);

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));

        verify(uptimeMonitorWrapper).getLastUptime();
        verify(inetAddressWrapper).getByName(HOST);
        verify(dispatcher).sendPdu(udpAddressWithPort(PORT), eq(SnmpConstants.version2c), eq(SecurityModel.SECURITY_MODEL_SNMPv2c),
                eq(COMMUNITY.getBytes()),
                eq(SecurityLevel.NOAUTH_NOPRIV),
                pduWithOidAndMessage(FULL_OID, "Custom Error Message"),
                eq(false));
    }

    @Test
    public void hostContextVariable() throws Exception {
        final String host = "custom.l7tech.com";
        assertion.setTargetHostname("${host}");
        initServerAssertion();
        context.setVariable("host", host);

        when(inetAddressWrapper.getByName(host)).thenReturn(address);

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));

        verify(uptimeMonitorWrapper).getLastUptime();
        verify(inetAddressWrapper).getByName(host);
        verify(dispatcher).sendPdu(udpAddressWithPort(PORT), eq(SnmpConstants.version2c), eq(SecurityModel.SECURITY_MODEL_SNMPv2c),
                eq(COMMUNITY.getBytes()),
                eq(SecurityLevel.NOAUTH_NOPRIV),
                pduWithOidAndMessage(FULL_OID, MESSAGE),
                eq(false));
    }

    @Test
    public void communityContextVariable() throws Exception {
        final String community = "custom";
        assertion.setCommunity("${community}");
        initServerAssertion();
        context.setVariable("community", community);

        when(inetAddressWrapper.getByName(HOST)).thenReturn(address);

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));

        verify(uptimeMonitorWrapper).getLastUptime();
        verify(inetAddressWrapper).getByName(HOST);
        verify(dispatcher).sendPdu(udpAddressWithPort(PORT), eq(SnmpConstants.version2c), eq(SecurityModel.SECURITY_MODEL_SNMPv2c),
                eq(community.getBytes()),
                eq(SecurityLevel.NOAUTH_NOPRIV),
                pduWithOidAndMessage(FULL_OID, MESSAGE),
                eq(false));
    }

    @Test
    public void oidContextVariable() throws Exception {
        final int oid = 5;
        assertion.setOid("${snmpOid}");
        initServerAssertion();
        context.setVariable("snmpOid", oid);

        when(inetAddressWrapper.getByName(HOST)).thenReturn(address);

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));

        verify(uptimeMonitorWrapper).getLastUptime();
        verify(inetAddressWrapper).getByName(HOST);
        verify(dispatcher).sendPdu(udpAddressWithPort(PORT), eq(SnmpConstants.version2c), eq(SecurityModel.SECURITY_MODEL_SNMPv2c),
                eq(COMMUNITY.getBytes()),
                eq(SecurityLevel.NOAUTH_NOPRIV),
                pduWithOidAndMessage(OID_PREFIX + oid, MESSAGE),
                eq(false));
    }

    @Test
    public void oidContextVariableNotNumeric() throws Exception {
        assertion.setOid("${snmpOid}");
        initServerAssertion();
        context.setVariable("snmpOid", "invalid");

        when(inetAddressWrapper.getByName(HOST)).thenReturn(address);

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));

        verify(uptimeMonitorWrapper).getLastUptime();
        verify(inetAddressWrapper).getByName(HOST);
        verify(dispatcher).sendPdu(udpAddressWithPort(PORT), eq(SnmpConstants.version2c), eq(SecurityModel.SECURITY_MODEL_SNMPv2c),
                eq(COMMUNITY.getBytes()),
                eq(SecurityLevel.NOAUTH_NOPRIV),
                pduWithOidAndMessage(OID_PREFIX + 1, MESSAGE),
                eq(false));
    }

    private void initServerAssertion() {
        serverAssertion = new ServerSnmpTrapAssertion(assertion);
        serverAssertion.setDispatcher(dispatcher);
        serverAssertion.setInetAddressWrapper(inetAddressWrapper);
        serverAssertion.setUptimeMonitorWrapper(uptimeMonitorWrapper);
    }

    private PDU pduWithOidAndMessage(final String oid, final String message) {
        return argThat(new PDUMatcher(oid, message));
    }

    private UdpAddress udpAddressWithPort(final int port) {
        return argThat(new UdpAddressMatcher(port));
    }


    private class UdpAddressMatcher extends ArgumentMatcher<UdpAddress> {
        private final int port;

        public UdpAddressMatcher(final int port) {
            this.port = port;
        }

        @Override
        public boolean matches(Object o) {
            final UdpAddress address = (UdpAddress) o;
            if (address.getPort() == port) {
                return true;
            }
            return false;
        }
    }

    private class PDUMatcher extends ArgumentMatcher<PDU> {
        private final String message;

        private final String oid;

        public PDUMatcher(final String oid, final String message) {
            this.message = message;
            this.oid = oid;
        }

        @Override
        public boolean matches(final Object obj) {
            final PDU pdu = (PDU) obj;
            final OID o = (OID) pdu.get(1).getVariable();
            final OctetString os = (OctetString) pdu.get(2).getVariable();
            if (o.toString().equals(oid) && os.toString().equals(message)) {
                return true;
            }
            return false;
        }
    }

}
