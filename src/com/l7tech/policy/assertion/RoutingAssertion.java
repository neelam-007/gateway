/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

import java.io.Serializable;

/**
 * @author alex
 */
public abstract class RoutingAssertion extends Assertion implements Cloneable, Serializable {
    // saml (model as a different bean when serializer supports it)
    protected boolean attachSamlSenderVouches;
    protected int samlAssertionExpiry = 5;
    protected boolean groupMembershipStatement;
    protected boolean taiCredentialChaining = false;

    public boolean isAttachSamlSenderVouches() {
        return attachSamlSenderVouches;
    }

    public void setAttachSamlSenderVouches(boolean attachSamlSenderVouches) {
        this.attachSamlSenderVouches = attachSamlSenderVouches;
    }

    public int getSamlAssertionExpiry() {
        return samlAssertionExpiry;
    }

    public void setSamlAssertionExpiry(int samlAssertionExpiry) {
        if (samlAssertionExpiry <= 0) {
            throw new IllegalArgumentException();
        }
        this.samlAssertionExpiry = samlAssertionExpiry;
    }

    public boolean isGroupMembershipStatement() {
        return groupMembershipStatement;
    }

    public void setGroupMembershipStatement(boolean groupMembershipStatement) {
        this.groupMembershipStatement = groupMembershipStatement;
    }

    public boolean isTaiCredentialChaining() {
        return taiCredentialChaining;
    }

    public void setTaiCredentialChaining(boolean taiCredentialChaining) {
        this.taiCredentialChaining = taiCredentialChaining;
    }
}

