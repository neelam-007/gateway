/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.proxy.policy.ClientPolicyFactory;
import com.l7tech.proxy.policy.assertion.ClientAssertion;
import com.l7tech.util.XmlSafe;

import java.io.Serializable;

/**
 * Holds a policy assertion tree along with versioning metadata.
 */
@XmlSafe
public class Policy implements Serializable, Cloneable {
    private Assertion assertion;
    private String version;
    private ClientAssertion clientAssertion;
    private boolean valid = true;
    private boolean alwaysValid = false;

    public Policy() {
    }

    public Policy(Assertion assertion, String version) {
        this.assertion = assertion;
        this.version = version;
    }

    public Assertion getAssertion() {
        return assertion;
    }

    public synchronized ClientAssertion getClientAssertion() throws PolicyAssertionException {
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
        return alwaysValid || valid;
    }

    /**
     * Note that this policy is known to have caused at least one PolicyAssertionException
     * and so should be ignored when processing messages.
     *
     * @param valid false to mark this policy as invalid.  (Bean serializer may set it to true when loading from disk.)
     */
    @XmlSafe
    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public boolean isAlwaysValid() {
        return alwaysValid;
    }

    @XmlSafe
    public void setAlwaysValid(boolean alwaysValid) {
        this.alwaysValid = alwaysValid;
    }

    /** assertion mutator for xml bean deserializer.  Do not call this method. */
    @XmlSafe
    public void setAssertion(Assertion assertion) {
        this.assertion = assertion;
    }

    /** Set the policy version. */
    @XmlSafe
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
