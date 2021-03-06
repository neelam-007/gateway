/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.assertion;

import com.l7tech.util.Functions;
import static com.l7tech.policy.assertion.AssertionMetadata.*;

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
    private Throwable cause = null;

    public UnknownAssertion() {
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

    public Throwable cause() {
        return cause;
    }

    private final static String baseName = "Unresolved assertion type";

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<UnknownAssertion>(){
        @Override
        public String getAssertionName( final UnknownAssertion assertion, final boolean decorate) {
            if(!decorate) return baseName;
            return assertion.getDetailMessage();
        }
    };

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();

        meta.put(SHORT_NAME, baseName);
        meta.put(DESCRIPTION, "This assertion type does not exist on this system.");
        meta.put(POLICY_NODE_ICON, "com/l7tech/console/resources/unknown.gif");
        meta.put(POLICY_NODE_NAME_FACTORY, policyNameFactory);
        meta.putNull( PROPERTIES_ACTION_FACTORY );

        return meta;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer(super.toString());
        if (detailMessage != null)
            sb.append(" detailMessage=" + detailMessage);
        if (originalXml != null)
            sb.append(" originalXml=" + originalXml);
        return sb.toString();
    }

}
