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
    public static final Component GATEWAY              = new Component(1000, "G", "SecureSpan Gateway", null);
    public static final Component GW_HARDWARE          = new Component( 100, "H", "Hardware", GATEWAY);
    public static final Component GW_SERVER            = new Component( 200, "S", "Server", GATEWAY);
    public static final Component GW_MESSAGE_PROCESSOR = new Component(  10, "M", "Message Processor", GW_SERVER);
    public static final Component GW_POLICY_SERVICE    = new Component(  20, "P", "Policy Service", GW_SERVER);
    public static final Component GW_TOKEN_SERVICE     = new Component(  30, "T", "Security Token Service", GW_SERVER);
    public static final Component GW_AUDIT_SYSTEM      = new Component(  40, "A", "Audit System", GW_SERVER);
    public static final Component GW_CLUSTER           = new Component( 300, "C", "Cluster", GATEWAY);
    public static final Component GW_DATABASE          = new Component( 400, "D", "Database", GATEWAY);

    public static final Component AGENT = new Component(2000, "A", "SecureSpan Agent", null);
    public static final Component MANAGER = new Component(3000, "M", "SecureSpan Manager", null);

    private Component(int localNum, String localCode, String name, Component parent) {
        this.localNum = localNum;
        this.localCode = localCode;
        this.name = name;
        this.parent = parent;

        if (componentsByCode == null) componentsByCode = new HashMap();
        componentsByCode.put(getCode(), this);
    }

    /** @return the Component that uses code, or null if not found. */
    public static Component fromCode(String code) {
        if (componentsByCode == null) return null;
        return (Component)componentsByCode.get(code);
    }

    public String getName() {
        return name;
    }

    public Component getParent() {
        return parent;
    }

    private static class Stuff {
        private int num;
        private String code;
    }

    private Stuff collect(Stuff stuff) {
        stuff.num += localNum;
        stuff.code = stuff.code == null ? localCode : localCode + stuff.code;
        if (parent == null)
            return stuff;
        else
            return parent.collect(stuff);
    }

    public String getCode() {
        Stuff stuff = collect(new Stuff());
        return stuff.code;
    }

    public int getNumber() {
        Stuff stuff = collect(new Stuff());
        return stuff.num;
    }

    public String toString() {
        return getCode() + " " + getName() + " (" + getNumber() + ")";
    }

    private final int localNum;
    private final String localCode;
    private final String name;
    private final Component parent;

    private static Map componentsByCode;
}
