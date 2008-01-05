/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion;

/**
 * Provides threat protection against code injection attacks targeting web
 * applications.
 *
 * <h2>Change History</h2>
 * Before 4.3, only one protection type allowed. Since 4.3, one or more protection types allowed.
 *
 * @author rmak
 * @since SecureSpan 3.7
 */
public class CodeInjectionProtectionAssertion extends Assertion {

    /** Whether to apply protections to request URL. */
    private boolean _includeRequestUrl;

    /** Whether to apply protections to request body. */
    private boolean _includeRequestBody;

    /** Whether to apply protections to response body. */
    private boolean _includeResponseBody;

    /** Protection types to apply. Replaces previous _protection since 4.3. */
    private CodeInjectionProtectionType[] _protections = new CodeInjectionProtectionType[0];

    public boolean isIncludeRequestUrl() {
        return _includeRequestUrl;
    }

    public void setIncludeRequestUrl(boolean b) {
        _includeRequestUrl = b;
    }

    public boolean isIncludeRequestBody() {
        return _includeRequestBody;
    }

    public void setIncludeRequestBody(boolean b) {
        _includeRequestBody = b;
    }

    public boolean isIncludeResponseBody() {
        return _includeResponseBody;
    }

    public void setIncludeResponseBody(boolean b) {
        _includeResponseBody = b;
    }

    public void setProtection(final CodeInjectionProtectionType protection) {
        _protections = new CodeInjectionProtectionType[]{protection};
    }

    /**
     * @return protection types to apply; never null
     */
    public CodeInjectionProtectionType[] getProtections() {
        return _protections;
    }

    /**
     * @param protections   protection types to apply; must not be <code>null</code>
     * @throws IllegalArgumentException if <code>protections</code> is <code>null</code>
     */
    public void setProtections(final CodeInjectionProtectionType[] protections) {
        if (protections == null)
            throw new IllegalArgumentException("protections array must not be null");
        _protections = protections;
    }
}
