package com.l7tech.external.assertions.snmptrap.server;

import com.l7tech.external.assertions.snmptrap.SnmpTrapAssertion;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.util.UptimeMetrics;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.snmp4j.MessageDispatcher;
import org.snmp4j.PDU;
import org.snmp4j.TransportMapping;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.security.SecurityModel;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.TimeTicks;
import org.snmp4j.smi.UdpAddress;

import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.junit.Assert.*;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ServerSnmpTrapAssertionTest {
    private static final String COMMUNITY = "l7";
    private static final String HOST = "test.l7tech.com";
    private static final String OID = "1";
    private static final String OID_PREFIX = "1.3.6.1.4.1.17304.7.3.";
    private static final String FULL_OID = OID_PREFIX + OID;
    private static final String MESSAGE = "my snmp message";
    private static final int PORT = 162;
    private static final long UPTIME = 0;
    private SnmpTrapAssertion assertion;
    private ServerSnmpTrapAssertion serverAssertion;
    private PolicyEnforcementContext context;
    @Mock
    private MessageDispatcher dispatcher;
    @Mock
    private InetAddress address;
    @Mock
    private UptimeMetrics uptimeMetrics;

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

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));

        verify(dispatcher).sendPdu(udpAddressWithPort(PORT), eq(SnmpConstants.version2c), eq(SecurityModel.SECURITY_MODEL_SNMPv2c),
                eq(COMMUNITY.getBytes()),
                eq(SecurityLevel.NOAUTH_NOPRIV),
                pduWithValues(UPTIME, FULL_OID, MESSAGE),
                eq(false));
    }

    @Test
    public void errorMessageContextVariable() throws Exception {
        assertion.setErrorMessage("${errMsg}");
        initServerAssertion();
        context.setVariable("errMsg", "Custom Error Message");

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));

        verify(dispatcher).sendPdu(udpAddressWithPort(PORT), eq(SnmpConstants.version2c), eq(SecurityModel.SECURITY_MODEL_SNMPv2c),
                eq(COMMUNITY.getBytes()),
                eq(SecurityLevel.NOAUTH_NOPRIV),
                pduWithValues(UPTIME, FULL_OID, "Custom Error Message"),
                eq(false));
    }

    @Test
    public void errorMessageContextVariableDoesNotExist() throws Exception {
        assertion.setErrorMessage("${errMsg}");
        initServerAssertion();

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));

        verify(dispatcher).sendPdu(udpAddressWithPort(PORT), eq(SnmpConstants.version2c), eq(SecurityModel.SECURITY_MODEL_SNMPv2c),
                eq(COMMUNITY.getBytes()),
                eq(SecurityLevel.NOAUTH_NOPRIV),
                pduWithValues(UPTIME, FULL_OID, ""),
                eq(false));
    }

    @Test
    public void hostContextVariable() throws Exception {
        final String host = "custom.l7tech.com";
        assertion.setTargetHostname("${host}");
        initServerAssertion();
        context.setVariable("host", host);

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));

        verify(dispatcher).sendPdu(udpAddressWithPort(PORT), eq(SnmpConstants.version2c), eq(SecurityModel.SECURITY_MODEL_SNMPv2c),
                eq(COMMUNITY.getBytes()),
                eq(SecurityLevel.NOAUTH_NOPRIV),
                pduWithValues(UPTIME, FULL_OID, MESSAGE),
                eq(false));
    }

    @Test
    public void hostContextVariableDoesNotExist() throws Exception {
        assertion.setTargetHostname("${host}");
        initServerAssertion();

        assertEquals(AssertionStatus.FAILED, serverAssertion.checkRequest(context));

        verify(dispatcher, never()).sendPdu(Matchers.<UdpAddress>any(), Matchers.anyInt(), Matchers.anyInt(),
                Matchers.<byte[]>any(),
                Matchers.anyInt(),
                Matchers.<PDU>any(),
                Matchers.anyBoolean());
    }

    @Test
    public void communityContextVariable() throws Exception {
        final String community = "custom";
        assertion.setCommunity("${community}");
        initServerAssertion();
        context.setVariable("community", community);

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));

        verify(dispatcher).sendPdu(udpAddressWithPort(PORT), eq(SnmpConstants.version2c), eq(SecurityModel.SECURITY_MODEL_SNMPv2c),
                eq(community.getBytes()),
                eq(SecurityLevel.NOAUTH_NOPRIV),
                pduWithValues(UPTIME, FULL_OID, MESSAGE),
                eq(false));
    }

    @Test
    public void communityContextVariableDoesNotExist() throws Exception {
        assertion.setCommunity("${community}");
        initServerAssertion();

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));

        verify(dispatcher).sendPdu(udpAddressWithPort(PORT), eq(SnmpConstants.version2c), eq(SecurityModel.SECURITY_MODEL_SNMPv2c),
                eq("".getBytes()),
                eq(SecurityLevel.NOAUTH_NOPRIV),
                pduWithValues(UPTIME, FULL_OID, MESSAGE),
                eq(false));
    }

    @Test
    public void oidContextVariable() throws Exception {
        final int oid = 5;
        assertion.setOid("${snmpOid}");
        initServerAssertion();
        context.setVariable("snmpOid", oid);

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));

        verify(dispatcher).sendPdu(udpAddressWithPort(PORT), eq(SnmpConstants.version2c), eq(SecurityModel.SECURITY_MODEL_SNMPv2c),
                eq(COMMUNITY.getBytes()),
                eq(SecurityLevel.NOAUTH_NOPRIV),
                pduWithValues(UPTIME, OID_PREFIX + oid, MESSAGE),
                eq(false));
    }

    @Test
    public void oidContextVariableNotNumeric() throws Exception {
        assertion.setOid("${snmpOid}");
        initServerAssertion();
        context.setVariable("snmpOid", "invalid");

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));

        verify(dispatcher).sendPdu(udpAddressWithPort(PORT), eq(SnmpConstants.version2c), eq(SecurityModel.SECURITY_MODEL_SNMPv2c),
                eq(COMMUNITY.getBytes()),
                eq(SecurityLevel.NOAUTH_NOPRIV),
                pduWithValues(UPTIME, OID_PREFIX + 1, MESSAGE),
                eq(false));
    }

     @Test
    public void oidContextVariableDoesNotExist() throws Exception {
        assertion.setOid("${snmpOid}");
        initServerAssertion();

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));

        verify(dispatcher).sendPdu(udpAddressWithPort(PORT), eq(SnmpConstants.version2c), eq(SecurityModel.SECURITY_MODEL_SNMPv2c),
                eq(COMMUNITY.getBytes()),
                eq(SecurityLevel.NOAUTH_NOPRIV),
                pduWithValues(UPTIME, OID_PREFIX + 1, MESSAGE),
                eq(false));
    }

    @Test
    public void nonZeroUptimeMetrics() throws Exception {
        initServerAssertion();

        when(uptimeMetrics.getDays()).thenReturn(1);
        when(uptimeMetrics.getHours()).thenReturn(1);
        when(uptimeMetrics.getMinutes()).thenReturn(1);

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));

        verify(uptimeMetrics).getDays();
        verify(uptimeMetrics).getHours();
        verify(uptimeMetrics).getMinutes();
        verify(dispatcher).sendPdu(udpAddressWithPort(PORT), eq(SnmpConstants.version2c), eq(SecurityModel.SECURITY_MODEL_SNMPv2c),
                eq(COMMUNITY.getBytes()),
                eq(SecurityLevel.NOAUTH_NOPRIV),
                pduWithValues(9006000L, FULL_OID, MESSAGE),
                eq(false));
    }

    private void initServerAssertion() {
        serverAssertion = new TestableServerSnmpTrapAssertion(assertion);
        serverAssertion.setDispatcher(dispatcher);
    }

    private PDU pduWithValues(final long uptime, final String oid, final String message) {
        return argThat(new PDUMatcher(uptime, oid, message));
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
        private final long uptime;
        private final String message;
        private final String oid;

        public PDUMatcher(final long uptime, final String oid, final String message) {
            this.uptime = uptime;
            this.message = message;
            this.oid = oid;
        }

        @Override
        public boolean matches(final Object obj) {
            boolean matches = false;
            final PDU pdu = (PDU) obj;
            final TimeTicks timeTicks = (TimeTicks) pdu.get(0).getVariable();
            final OID oidVariable = (OID) pdu.get(1).getVariable();
            final OctetString octetString = (OctetString) pdu.get(2).getVariable();
            if (timeTicks.getValue() == uptime && oidVariable.toString().equals(oid) && octetString.toString().equals(message)) {
                matches = true;
            }
            return matches;
        }
    }

    /**
     * Stubs static calls.
     */
    private class TestableServerSnmpTrapAssertion extends ServerSnmpTrapAssertion {
        public TestableServerSnmpTrapAssertion(final SnmpTrapAssertion ass) {
            super(ass);
        }

        @Override
        InetAddress getInetAddressByName(final String host) throws UnknownHostException {
            return address;
        }

        @Override
        UptimeMetrics getLastUptime() throws FileNotFoundException, IllegalStateException {
            return uptimeMetrics;
        }
    }

}
