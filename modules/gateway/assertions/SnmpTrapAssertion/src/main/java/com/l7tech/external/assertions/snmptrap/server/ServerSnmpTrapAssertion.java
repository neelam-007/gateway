package com.l7tech.external.assertions.snmptrap.server;

import com.l7tech.external.assertions.snmptrap.SnmpTrapAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.util.UptimeMonitor;
import com.l7tech.util.Charsets;
import com.l7tech.util.UptimeMetrics;
import org.snmp4j.MessageDispatcher;
import org.snmp4j.MessageDispatcherImpl;
import org.snmp4j.MessageException;
import org.snmp4j.PDU;
import org.snmp4j.mp.MPv2c;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.security.SecurityModel;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Server side implementation of assertion that sends an SNMP trap.
 */
public class ServerSnmpTrapAssertion extends AbstractServerAssertion<SnmpTrapAssertion> implements ServerAssertion<SnmpTrapAssertion> {
    public static final int[] OID_BASE = new int[] {1,3,6,1,4,1,17304,7,3};

    private final SnmpTrapAssertion ass;
    private final OID trapOid;
    private final OID messageOid;
    private final MessageDispatcher dispatcher;
    private final byte[] communityBytes;
    private final String[] varsUsed;

    public ServerSnmpTrapAssertion(SnmpTrapAssertion ass) {
        super(ass);

        this.ass = ass;
        dispatcher = new MessageDispatcherImpl();
        dispatcher.addMessageProcessingModel(new MPv2c());
        try {
            dispatcher.addTransportMapping(new DefaultUdpTransportMapping());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        SecurityProtocols.getInstance().addDefaultProtocols();

        int[] trapOi = new int[OID_BASE.length + 1];
        int[] msgOi = new int[OID_BASE.length + 1];
        System.arraycopy(OID_BASE, 0, trapOi, 0, OID_BASE.length);
        System.arraycopy(OID_BASE, 0, msgOi, 0, OID_BASE.length);
        int lastPart = ass.getOid();
        if (lastPart == 0) {
            logAndAudit( AssertionMessages.SNMP_BAD_TRAP_OID );
            lastPart = 1;
        }
        trapOi[trapOi.length - 1] = lastPart;
        msgOi[msgOi.length - 1] = 0;
        trapOid = new OID(trapOi);
        messageOid = new OID(msgOi);
        communityBytes = ass.getCommunity().getBytes();
        varsUsed = ass.getVariablesUsed();
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        PDU pdu = new PDU();
        pdu.setType(PDU.TRAP);
        UptimeMetrics um = UptimeMonitor.getLastUptime();
        long uptimeSeconds;
        if (um == null)
            uptimeSeconds = 0L;
        else
            uptimeSeconds = (long) ((um.getDays() * 86400) + (um.getHours() * 60 * 60) + (um.getMinutes() * 60));

        pdu.add(new VariableBinding(SnmpConstants.sysUpTime, new TimeTicks(uptimeSeconds * 100L ))); // TimeTicks is s/100
        pdu.add(new VariableBinding(SnmpConstants.snmpTrapOID, trapOid));

        String body = ExpandVariables.process(ass.getErrorMessage(), context.getVariableMap(varsUsed, getAudit()), getAudit());
        OctetString errorMessage;
        errorMessage = new OctetString(body.getBytes(Charsets.UTF8));
        pdu.add(new VariableBinding(messageOid, errorMessage));

        try {
            // TODO consider caching this DNS lookup for a while (just not forever).
            // As is, we just hope that one of the JRE, resolver, libc, or OS provide caching for gethostbyname().
            UdpAddress udpAddress = new UdpAddress(InetAddress.getByName(ass.getTargetHostname()), ass.getTargetPort());
            dispatcher.sendPdu(udpAddress,
                               SnmpConstants.version2c,
                               SecurityModel.SECURITY_MODEL_SNMPv2c,
                               communityBytes,
                               SecurityLevel.NOAUTH_NOPRIV,
                               pdu,
                               false);
        } catch (MessageException e) {
            logAndAudit( AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{ "The configured SNMP server hostname could not be found: " + e.getMessage() }, e );
            return AssertionStatus.FAILED;
        } catch (UnknownHostException e) {
            logAndAudit( AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{ "The configured SNMP server could not be reached: " + e.getMessage() }, e );
            return AssertionStatus.FAILED;
        }

        // We didn't detect early failure, so the datagram is on its way.
        // TODO does snmp4j provide a guarantee that the trap arrived Ok?
        return AssertionStatus.NONE;
    }
}
