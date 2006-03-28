/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
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
    private final String detailMessage;
    private final String originalXml;
    private final Throwable cause;

    public UnknownAssertion() {
        this(null, null, null);
    }

    public UnknownAssertion(String detailMessage) {
        this(detailMessage, null);
    }

    public UnknownAssertion(String detailMessage, String originalXml) {
        this(detailMessage, originalXml, null);
    }

    public UnknownAssertion(String detailMessage, String originalXml, Exception cause) {
        this.detailMessage = detailMessage == null ? "Unknown assertion" : detailMessage;
        this.originalXml = originalXml;
        this.cause = cause;
    }

    public static UnknownAssertion create(String unknownAssertionName, String originalXml) {
        return new UnknownAssertion("Unknown assertion: " + unknownAssertionName, originalXml);
    }

    public static UnknownAssertion create(String unknownAssertionName, String originalXml, ClassNotFoundException e) {
        return new UnknownAssertion("Code not available for assertion: " + unknownAssertionName, originalXml, e);
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

    public String getOriginalXml() {
        return originalXml;
    }

    public Throwable getCause() {
        return cause;
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
