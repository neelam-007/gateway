/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

/**
 * The <code>UnknownAssertion</code> is an assertion that indicates
 * that there is an unknown assertion in the policy tree. This assertion
 * should always return a negative result.
 *
 * @author <a href="mailto:emarceta@layer7tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class UnknownAssertion extends Assertion {
    private String detailMessage;

    /**
     * An optional detail message for this unknown assertion. It will
     * typically describe the cause of unknown assertion.
     *
     * @return the unknown assertin description
     */
    public String getDetailMessage() {
        return detailMessage;
    }

    /**
     * @param detailMessage the unknown assertion detail message
     */
    public void setDetailMessage(String detailMessage) {
        this.detailMessage = detailMessage;
    }
}
