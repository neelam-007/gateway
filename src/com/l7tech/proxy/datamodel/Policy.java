/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.proxy.policy.ClientPolicyFactory;
import com.l7tech.proxy.policy.assertion.ClientAssertion;

import java.io.Serializable;

/**
 * Holds a policy assertion tree along with versioning metadata.
 */
public class Policy implements Serializable, Cloneable {
    private Assertion assertion;
    private String version;
    private ClientAssertion clientAssertion;
    private boolean valid = true;
    private boolean persistent = false;

    public Policy() {
    }

    public Policy(Assertion assertion, String version) {
        this.assertion = assertion;
        this.version = version;
    }

    public Assertion getAssertion() {
        return assertion;
    }

    public synchronized ClientAssertion getClientAssertion() {
        if (clientAssertion != null)
            return clientAssertion;
        if (assertion != null)
            return clientAssertion = ClientPolicyFactory.getInstance().makeClientPolicy(assertion);
        return null;
    }

    /** @return the policy version string from the SSG, or null if not applicable. */
    public String getVersion() {
        return version;
    }

    /**
     * Check if this policy has ever caused a PolicyAssertionException.  If it has, you are better off
     * ignoring it when processing messages in the hope that the SSG will send you a new one.
     *
     * @return false if this policy has ever caused a PolicyAssertionException; otherwise true.
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Note that this policy is known to have caused at least one PolicyAssertionException
     * and so should be ignored when processing messages.
     *
     * @param valid false to mark this policy as invalid.  (Bean serializer may set it to true when loading from disk.)
     */
    public void setValid(boolean valid) {
        this.valid = valid;
    }

    /**
     * Check if this Policy should be routed to the persistent policy store next time it is saved in the root
     * {@link PolicyManager}.
     *
     * @return true iff. this Policy should be saved to disk
     */
    public boolean isPersistent() {
        return persistent;
    }

    /**
     * If true, this Policy will be routed to the persistent policy store next time it is saved in the root
     * {@link PolicyManager}.
     *
     * @param persistent  true to save this policy to disk.
     */
    public void setPersistent(boolean persistent) {
        this.persistent = persistent;
    }

    /** assertion mutator for xml bean deserializer.  Do not call this method. */
    public void setAssertion(Assertion assertion) {
        this.assertion = assertion;
    }

    /** Set the policy version. */
    public void setVersion(String version) {
        this.version = version;
    }

    public Object clone() throws CloneNotSupportedException {
        Policy p = new Policy(this.assertion.getCopy(), this.version);
        return p;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Policy)) return false;

        final Policy policy = (Policy)o;

        if (assertion != null ? !assertion.equals(policy.assertion) : policy.assertion != null) return false;
        if (version != null ? !version.equals(policy.version) : policy.version != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (assertion != null ? assertion.hashCode() : 0);
        result = 29 * result + (version != null ? version.hashCode() : 0);
        return result;
    }
}
