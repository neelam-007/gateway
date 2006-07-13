/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.assertion.credential;

/**
 * A credential source assertion that gathers from an HTTP cookie.
 */
public class CookieCredentialSourceAssertion {
    public String cookieName;

    public String getCookieName() {
        return cookieName;
    }

    public void setCookieName(String cookieName) {
        this.cookieName = cookieName;
    }
}
