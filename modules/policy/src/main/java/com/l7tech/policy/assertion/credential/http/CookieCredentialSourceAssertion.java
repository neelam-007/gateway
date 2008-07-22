/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.assertion.credential.http;

import com.l7tech.policy.assertion.Assertion;

/**
 * A credential source assertion that gathers from an HTTP cookie.
 */
public class CookieCredentialSourceAssertion extends Assertion {
    public String cookieName = "session";

    public String getCookieName() {
        return cookieName;
    }

    public void setCookieName(String cookieName) {
        if (cookieName == null || cookieName.length() < 1) throw new IllegalArgumentException("Cookie name must be non-empty.");
        this.cookieName = cookieName;
    }

    public boolean isCredentialSource() {
        return true;
    }
}
