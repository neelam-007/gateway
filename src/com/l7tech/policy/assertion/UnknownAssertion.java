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
    private String originalXml = null;
    private Throwable problemEncountered = null;

    public UnknownAssertion() {
    }

    public UnknownAssertion(String detailMessage) {
        this.detailMessage = detailMessage;
    }

    public UnknownAssertion(Throwable problemEncountered, String originalXml) {
        this.detailMessage = "Unknown assertion '" + problemEncountered.getMessage() + "'";
        this.originalXml = originalXml;
    }

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

    public String getOriginalXml() {
        return originalXml;
    }

    public void setOriginalXml(String originalXml) {
        this.originalXml = originalXml;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer(super.toString());
        if (detailMessage != null)
            sb.append(" detailMessage=" + detailMessage);
        if (originalXml != null)
            sb.append(" originalXml=" + originalXml);
        return sb.toString();
    }

}
