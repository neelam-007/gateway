/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

import java.util.EventObject;

/**
 * Object encapsulating an event involving a change in state of an Ssg, which might be of interest
 * to an SsgListener.
 *
 * At the moment only policy attachment events are considered to be of interest.
 *
 * User: mike
 * Date: Sep 3, 2003
 * Time: 10:00:14 AM
 */
public class SsgEvent extends EventObject {
    /** Typesafe enum for the types of events we can describe. */
    private static class SsgEventType {
        private final int code;
        private final String desc;
        private SsgEventType(int num, String d) { code = num; desc = d; }
        public int getCode() { return code; }
        public String toString() { return desc; }
    }

    /** The types of events we can describe. */
    public static final SsgEventType POLICY_ATTACHED = new SsgEventType(1, "policy added");
    public static final SsgEventType DATA_CHANGED = new SsgEventType(2, "data changed");

    /** Type of this event, chosen from the list above. */
    private final SsgEventType type;

    /** For POLICY_* events, the policy that was attached. */
    private Policy policy;

    private SsgEvent(Ssg source, SsgEventType type, Policy policy) {
        super(source);
        this.policy = policy;
        this.type = type;
    }

    private SsgEvent(Ssg source, SsgEventType type) {
        super(source);
        this.type = type;
    }

    public SsgEventType getType() {
        return type;
    }

    public Policy getPolicy() {
        return policy;
    }

    /** Create a new SsgEvent of type POLICY_ATTACHED. */
    public static SsgEvent createPolicyAttachedEvent(Ssg ssg, Policy policy) {
        SsgEvent evt = new SsgEvent(ssg, POLICY_ATTACHED, policy);
        return evt;
    }

    /** Create a new SsgEvent of type DATA_CHANGED. */
    public static SsgEvent createDataChangedEvent(Ssg ssg) {
        SsgEvent evt = new SsgEvent(ssg, DATA_CHANGED);
        return evt;
    }

    /**
     * Returns a String representation of this SsgEvent.
     *
     * @return  A a String representation of this SsgEvent.
     */
    public String toString() {
        return getClass().getName() + "[source=" + source + ", type=" + type + ", policy=" + policy + "]";
    }
}
