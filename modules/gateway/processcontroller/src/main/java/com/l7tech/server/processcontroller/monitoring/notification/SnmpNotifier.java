/*
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller.monitoring.notification;

import com.l7tech.server.audit.Auditor;
import com.l7tech.server.audit.LogOnlyAuditor;
import com.l7tech.server.management.config.monitoring.SnmpTrapNotificationRule;
import com.l7tech.server.management.config.monitoring.Trigger;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.util.UptimeMonitor;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.UptimeMetrics;
import org.snmp4j.MessageDispatcher;
import org.snmp4j.MessageDispatcherImpl;
import org.snmp4j.PDU;
import org.snmp4j.mp.MPv2c;
import org.snmp4j.mp.PduHandle;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.security.SecurityModel;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
class SnmpNotifier extends Notifier<SnmpTrapNotificationRule> {
    private static final Logger logger = Logger.getLogger(SnmpNotifier.class.getName());

    public static final int[] OID_BASE = new int[] {1,3,6,1,4,1,17304,7,3};

    private final Auditor auditor;
    private final OID trapOid;
    private final OID messageOid;
    private final DefaultUdpTransportMapping transportMapping;
    private final MessageDispatcher dispatcher;
    private final byte[] communityBytes;
    private final String snmpHost;
    private final int snmpPort;
    private final String text;

    protected SnmpNotifier(SnmpTrapNotificationRule rule) {
        super(rule);
        dispatcher = new MessageDispatcherImpl();
        dispatcher.addMessageProcessingModel(new MPv2c());
        try {
            transportMapping = new DefaultUdpTransportMapping();
            dispatcher.addTransportMapping(transportMapping);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        SecurityProtocols.getInstance().addDefaultProtocols();

        int[] trapOi = new int[OID_BASE.length + 1];
        int[] msgOi = new int[OID_BASE.length + 1];
        System.arraycopy(OID_BASE, 0, trapOi, 0, OID_BASE.length);
        System.arraycopy(OID_BASE, 0, msgOi, 0, OID_BASE.length);

        int lastPart = rule.getOidSuffix();
        if (lastPart == 0)
            throw new IllegalArgumentException("OID suffix must not be zero");

        trapOi[trapOi.length - 1] = lastPart;
        msgOi[msgOi.length - 1] = 0;
        trapOid = new OID(trapOi);
        messageOid = new OID(msgOi);
        communityBytes = rule.getCommunity().getBytes();
        snmpHost = rule.getSnmpHost();
        snmpPort = rule.getPort();
        text = rule.getText();
        if (text == null)
            throw new IllegalArgumentException("text must be non-null");
        auditor = new LogOnlyAuditor(logger);
    }

    public void doNotification(Long timestamp, Object value, Trigger trigger) throws IOException {
        PDU pdu = new PDU();
        pdu.setType(PDU.TRAP);
        UptimeMetrics um = UptimeMonitor.getLastUptime();
        long uptimeSeconds;
        if (um == null)
            uptimeSeconds = 0;
        else
            uptimeSeconds = (um.getDays() * 86400) + (um.getHours() * 60 * 60) + (um.getMinutes() * 60);

        pdu.add(new VariableBinding(SnmpConstants.sysUpTime, new TimeTicks(uptimeSeconds * 100))); // TimeTicks is s/100
        pdu.add(new VariableBinding(SnmpConstants.snmpTrapOID, trapOid));

        Map<String, String> variables = getMonitoringVariables(trigger);

        String body = ExpandVariables.process(text, variables, auditor);
        OctetString errorMessage;
        try {
            errorMessage = new OctetString(body.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e); // Can't happen
        }
        pdu.add(new VariableBinding(messageOid, errorMessage));

        // TODO consider caching this DNS lookup for a while (just not forever).
        // As is, we just hope that one of the JRE, resolver, libc, or OS provide caching for gethostbyname().
        UdpAddress udpAddress = new UdpAddress(InetAddress.getByName(snmpHost), snmpPort);
        PduHandle handle = dispatcher.sendPdu(udpAddress,
                                              SnmpConstants.version2c,
                                              SecurityModel.SECURITY_MODEL_SNMPv2c,
                                              communityBytes,
                                              SecurityLevel.NOAUTH_NOPRIV,
                                              pdu,
                                              false);
    }

    @Override
    public void close() {
        try {
            transportMapping.close();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Unable to close SNMP transport mapping: " + ExceptionUtils.getMessage(e), e);
        }
    }
}
