/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion.alert;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.audit.Auditor;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.alert.SnmpTrapAssertion;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;
import org.snmp4j.MessageDispatcher;
import org.snmp4j.MessageDispatcherImpl;
import org.snmp4j.MessageException;
import org.snmp4j.PDU;
import org.snmp4j.mp.MPv2c;
import org.snmp4j.mp.PduHandle;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.security.SecurityModel;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Logger;

/**
 * Server side implementation of assertion that sends an SNMP trap.
 */
public class ServerSnmpTrapAssertion implements ServerAssertion {
    private static final Logger logger = Logger.getLogger(ServerSnmpTrapAssertion.class.getName());
    public static final int[] OID_BASE = new int[] {1,3,6,1,4,1,17304,7,3};

    private final ApplicationContext applicationContext;
    private final Auditor auditor;
    private final SnmpTrapAssertion ass;
    private final OID oid;
    private final MessageDispatcher dispatcher;
    private final byte[] communityBytes;
    private final OctetString errorMessage;

    public ServerSnmpTrapAssertion(SnmpTrapAssertion ass, ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        auditor = new Auditor(this, applicationContext, logger);
        this.ass = ass;
        dispatcher = new MessageDispatcherImpl();
        dispatcher.addMessageProcessingModel(new MPv2c());
        try {
            dispatcher.addTransportMapping(new DefaultUdpTransportMapping());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        SecurityProtocols.getInstance().addDefaultProtocols();

        int[] oi = new int[OID_BASE.length + 1];
        oi[oi.length - 1] = ass.getOid();
        oid = new OID(oi);
        communityBytes = ass.getCommunity().getBytes();
        errorMessage = new OctetString(ass.getErrorMessage());
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        PDU pdu = new PDU();
        pdu.setType(PDU.TRAP);

        pdu.add(new VariableBinding(oid, errorMessage));

        try {
            // TODO consider caching this DNS lookup for a while (just not forever).
            // As is, we just hope that one of the JRE, resolver, libc, or OS provide caching for gethostbyname().
            UdpAddress udpAddress = new UdpAddress(InetAddress.getByName(ass.getTargetHostname()), ass.getTargetPort());
            PduHandle handle = dispatcher.sendPdu(udpAddress,
                                                  SnmpConstants.version2c,
                                                  SecurityModel.SECURITY_MODEL_SNMPv2c,
                                                  communityBytes,
                                                  SecurityLevel.NOAUTH_NOPRIV,
                                                  pdu,
                                                  false);
        } catch (MessageException e) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {"The configured SNMP server hostname could not be found: " + e.getMessage()}, e);
            return AssertionStatus.FAILED;
        } catch (UnknownHostException e) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {"The configured SNMP server could not be reached: " + e.getMessage()}, e);
            return AssertionStatus.FAILED;
        }

        // We didn't detect early failure, so the datagram is on its way.
        // TODO does snmp4j provide a guarantee that the trap arrived Ok?
        return AssertionStatus.NONE;
    }
}
