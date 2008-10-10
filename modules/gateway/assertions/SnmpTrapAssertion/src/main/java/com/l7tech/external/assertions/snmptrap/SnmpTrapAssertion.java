/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.external.assertions.snmptrap;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.UsesVariables;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.variable.Syntax;

/**
 * An assertion that sends an SNMP trap.
 */
public class SnmpTrapAssertion extends Assertion implements UsesVariables {
    public static final int DEFAULT_PORT = 162;
    public static final String DEFAULT_ERROR_MESSAGE = "Layer 7 SecureSpan Gateway SNMP Trap";

    private String targetHostname = "";
    private int targetPort = DEFAULT_PORT;
    private String community = "";
    private String errorMessage = DEFAULT_ERROR_MESSAGE;
    private int lastOidComponent = 0;

    /**
     * Create an SnmpTrapAssertion with default settings.
     */
    public SnmpTrapAssertion() {
    }

    /**
     * Create an SnmpTrapAssertion with the specified settings.  The default target port and SNMP object ID will be used.
     *
     * @param targetHostname   the hostname or IP address to which the SNMP trap should be sent.  Must not be null or empty.
     * @param errorMessage     the error message to send in the trap, or null to use a default error message.
     * @param community        the SNMP community name.  Must not be null.
     */
    public SnmpTrapAssertion(String targetHostname, String errorMessage, String community) {
        if (targetHostname == null) throw new NullPointerException("SNMP target hostname must not be null");
        if (community == null) throw new NullPointerException("SNMP community must not be null");
        this.targetHostname = targetHostname;
        this.community = community;
        if (errorMessage != null)
            this.errorMessage = errorMessage;
    }

    /**
     * Create an SnmpTrapAssertion with the fully specified settings.
     *
     * @param targetHostname   the hostname or IP address to which the SNMP trap should be sent.  Must not be null or empty.
     * @param targetPort       the port to use, or 0 to detault to 162.
     * @param community        the SNMP community name.  Must not be null.
     * @param oid              the final component of the SNMP object ID to use to identify the error message.
     * @param errorMessage     the error message to send in the trap, or null to use a default error message.
     */
    public SnmpTrapAssertion(String targetHostname, int targetPort, String community, int oid, String errorMessage) {
        if (targetHostname == null) throw new NullPointerException("SNMP target hostname must not be null");
        if (targetHostname.length() < 1) throw new IllegalArgumentException("SNMP target hostname must not be empty");
        if (community == null) throw new NullPointerException("SNMP community must not be null");
        if (targetPort > 65535) throw new IllegalArgumentException("SNMP target port out of range");

        this.targetHostname = targetHostname;
        this.community = community;
        if (errorMessage != null)
            this.errorMessage = errorMessage;
        this.lastOidComponent = oid;
        if (targetPort > 0)
            this.targetPort = targetPort;
    }

    public String getTargetHostname() {
        return targetHostname;
    }

    public void setTargetHostname(String targetHostname) {
        if (targetHostname == null) throw new NullPointerException("SNMP target hostname must not be null");
        this.targetHostname = targetHostname;
    }

    public int getTargetPort() {
        return targetPort;
    }

    public void setTargetPort(int targetPort) {
        if (targetPort < 1 || targetPort > 65535) throw new IllegalArgumentException("SNMP target port out of range");
        this.targetPort = targetPort;
    }

    public String getCommunity() {
        return community;
    }

    public void setCommunity(String community) {
        if (community == null) throw new NullPointerException("SNMP community must not be null");
        this.community = community;
    }

    public int getOid() {
        return lastOidComponent;
    }

    public void setOid(int oid) {
        this.lastOidComponent = oid;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        if (errorMessage == null) throw new NullPointerException("SNMP trap error message must not be null");
        this.errorMessage = errorMessage;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer(super.toString());
        sb.append(" to host ");
        sb.append(targetHostname);
        sb.append(" on port ");
        sb.append(targetPort);
        sb.append(" of message \"");
        sb.append(errorMessage);
        sb.append('"');
        return sb.toString();
    }

    public String[] getVariablesUsed() {
        return Syntax.getReferencedNames(errorMessage);
    }

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();

        meta.put(AssertionMetadata.SHORT_NAME, "Send SNMP Trap");
        meta.put(AssertionMetadata.LONG_NAME, "Send SNMP Trap");
        meta.put(AssertionMetadata.DESCRIPTION, "Sends an SNMP trap to a specified Simple Network Management Protocol server.");

        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "audit" });
        meta.put(AssertionMetadata.PALETTE_NODE_NAME, "Send SNMP Trap");
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/Edit16.gif");
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/Edit16.gif");

        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "SNMP Trap Properties");

        return meta;
    }
}
