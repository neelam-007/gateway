/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common;

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
    public static final Component     GW_POLICY_SERVICE   = new Component(  20000, "Policy Service", GW_SERVER);
    public static final Component     GW_TOKEN_SERVICE    = new Component(  30000, "Security Token Service", GW_SERVER);
    public static final Component     GW_AUDIT_SYSTEM     = new Component(  40000, "Audit System", GW_SERVER);
    public static final Component     GW_CLUSTER          = new Component( 300000, "Cluster", GATEWAY);
    public static final Component     GW_DATABASE         = new Component( 400000, "Database", GATEWAY);

    public static final Component BRIDGE = new Component(2000000, "SecureSpan Bridge", null);
    public static final Component MANAGER = new Component(3000000, "SecureSpan Manager", null);

    public Component(int localNum, String name, Component parent) {
        this.localNum = localNum;
        this.name = name;
        this.parent = parent;

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

    private static Map componentsById;
}
