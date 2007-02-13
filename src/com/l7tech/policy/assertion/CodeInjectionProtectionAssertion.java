/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion;

/**
 * Provides threat protection against code injection attacks targeting web
 * applications.
 *
 * @author rmak
 * @since SecureSpan 3.7
 */
public class CodeInjectionProtectionAssertion extends Assertion {

    /** Whether to apply protection to request URL. */
    private boolean _includeRequestUrl;

    /** Whether to apply protection to request body. */
    private boolean _includeRequestBody;

    /** Whether to apply protection to response body. */
    private boolean _includeResponseBody;

    /** Type of protection. */
    private CodeInjectionProtectionType _protection;

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

    public CodeInjectionProtectionType getProtection() {
        return _protection;
    }

    public void setProtection(final CodeInjectionProtectionType protection) {
        _protection = protection;
    }
}
