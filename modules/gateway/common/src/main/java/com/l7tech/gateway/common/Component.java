/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.gateway.common;

import java.util.HashMap;
import java.util.Map;

/**
 * @author alex
 * @version $Revision$
 */
public class Component {
    public static final Component GATEWAY                 = new Component(1000000, "SecureSpan Gateway", null);
    public static final Component   GW_HARDWARE           = new Component( 100000, "Hardware", GATEWAY);
    public static final Component   GW_SERVER             = new Component( 200000, "Server", GATEWAY);
    public static final Component     GW_POLICY           = new Component(  10000, "Service Policies", GW_SERVER);
    public static final Component       GW_POLICY_CRED    = new Component(   1000, "Credential Sources", GW_POLICY);
    public static final Component         HTTP_BASIC      = new Component(    100, "HTTP Basic Authentication", GW_POLICY_CRED);
    public static final Component         HTTP_DIGEST     = new Component(    200, "HTTP Digest Authentication", GW_POLICY_CRED);
    public static final Component         HTTPS_CERT      = new Component(    300, "HTTPS Client Certificate Authentication", GW_POLICY_CRED);
    public static final Component         SAML            = new Component(    400, "SAML", GW_POLICY_CRED);
    public static final Component       GW_MSG_IDENTITY   = new Component(   2000, "Identity Assertions", GW_POLICY);
    public static final Component       GW_MSG_XMLSEC     = new Component(   3000, "XML Security Assertions", GW_POLICY);
    public static final Component       GW_MSG_ROUTING    = new Component(   5000, "Routing Assertions", GW_POLICY);
    public static final Component     GW_POLICY_SERVICE   = new Component(  20000, "Policy Service", GW_SERVER, true);
    public static final Component     GW_TOKEN_SERVICE    = new Component(  30000, "Security Token Service", GW_SERVER, true);
    public static final Component     GW_AUDIT_SYSTEM     = new Component(  40000, "Audit System", GW_SERVER);
    public static final Component     GW_AUDIT_SINK_CONFIG = new Component(  40010, "Audit Sink Configuration", GW_AUDIT_SYSTEM);
    public static final Component     GW_LICENSE_MANAGER  = new Component(  50000, "License Manager", GW_SERVER);
    public static final Component     GW_ADMINWS          = new Component(  60000, "Admin Web Service", GW_SERVER);
    public static final Component     GW_HTTPRECV         = new Component(  65000, "HTTP Listeners", GW_SERVER);
    public static final Component     GW_JMSRECV          = new Component(  70000, "JMS Receiver", GW_SERVER);
    public static final Component     GW_MQ_NATIVE_RECV   = new Component(  70100, "MQ Native Receiver", GW_SERVER);
    public static final Component     GW_EMAILRECV        = new Component(  75000, "Email Listeners", GW_SERVER);
    public static final Component     GW_ADMINAPPLET      = new Component(  80000, "Admin Applet", GW_SERVER, true);
    public static final Component     GW_FTPSERVER        = new Component(  90000, "FTP Server", GW_SERVER);
    public static final Component     GW_TRUST_STORE      = new Component(  90010, "Trusted Certificate Store", GW_SERVER);
    public static final Component     GW_SYSLOG           = new Component(  90020, "Syslog Client", GW_SERVER);
    public static final Component     GW_CSR_SERVLET      = new Component(  90030, "Certificate Signing Service", GW_SERVER);
    public static final Component     GW_UDDI_SERVICE     = new Component(  90040, "UDDI Service", GW_SERVER);
    public static final Component     GW_PASSWD_POLICY_MGR= new Component(  90050, "Password Policy Service", GW_SERVER);
    public static final Component     GW_ACCOUNT_MANAGER  = new Component(  90060, "Administrator account maintenance", GW_SERVER);
    public static final Component     GW_SFTP_POLL_RECV   = new Component(  90070, "SFTP Polling Listeners", GW_SERVER);
    public static final Component     GW_SSHRECV          = new Component(  90080, "SSH2 Listeners", GW_SERVER);
    public static final Component     GW_GENERIC_CONNECTOR= new Component(  90090, "Other Listeners", GW_SERVER);
    public static final Component     GW_BUNDLE_INSTALLER = new Component(  90100, "Bundle Installer", GW_SERVER);
    public static final Component     GW_BUNDLE_EXPORTER  = new Component(  90110, "Bundle Exporter", GW_SERVER);
    public static final Component     GW_SERVER_MODULE_FILE = new Component(  90120, "Server Module File Manager", GW_SERVER);
    // TODO find a way to atomically renumber these in an SQL script or UpgradeTask, this is ridiculous
    public static final Component   GW_CLUSTER            = new Component( 300000, "Cluster", GATEWAY);
    public static final Component   GW_DATABASE           = new Component( 400000, "Database", GATEWAY);

    public static final Component BRIDGE = new Component(2000000, "SecureSpan XML VPN Client", null);
    public static final Component MANAGER = new Component(3000000, "SecureSpan Manager", null);
    public static final Component ENTERPRISE_MANAGER = new Component(4000000, "Enterprise Service Manager", null);

    public Component(int localNum, String name, Component parent) {
        this(localNum, name, parent, false);
    }

    public Component(int localNum, String name, Component parent, boolean clientComponent) {
        this.localNum = localNum;
        this.name = name;
        this.parent = parent;
        this.clientComponent = clientComponent;

        if (componentsById == null) componentsById = new HashMap();
        Object old = componentsById.put(new Integer(getId()), this);
        if (old != null) throw new IllegalStateException("A component with id " + getId() + " has already been registered");
    }

    /** @return the Component that uses code, or null if not found. */
    public static Component fromId(int id) {
        if (componentsById == null) return null;
        return (Component)componentsById.get(new Integer(id));
    }

    public String getName() {
        return name;
    }

    public Component getParent() {
        return parent;
    }

    public boolean isClientComponent() {
        return clientComponent;
    }

    private static class Stuff {
        private int num;
    }

    private Stuff collect(Stuff stuff) {
        stuff.num += localNum;
        if (parent == null)
            return stuff;
        else
            return parent.collect(stuff);
    }

    public int getId() {
        Stuff stuff = collect(new Stuff());
        return stuff.num;
    }

    public String toString() {
        return getName() + " (" + getId() + ")";
    }

    private final int localNum;
    private final String name;
    private final Component parent;
    private final boolean clientComponent;

    private static Map componentsById;
}