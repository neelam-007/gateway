/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.assertion.credential.http;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.util.Functions;

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

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"accessControl"});

        meta.put(AssertionMetadata.SHORT_NAME, "Require HTTP Cookie");
        meta.put(AssertionMetadata.LONG_NAME, "Gather credentials in the form of an HTTP session cookie.");
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/authentication.gif");
        meta.put(AssertionMetadata.USED_BY_CLIENT, Boolean.TRUE);
        meta.put(AssertionMetadata.PALETTE_NODE_CLIENT_ICON, "com/l7tech/proxy/resources/tree/authentication.gif");

        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, new Functions.Unary<String, Assertion>(){
            public String call(Assertion assertion) {
                return "Require HTTP Cookie (name=" + getCookieName() + ")";
            }
        });

        meta.put(AssertionMetadata.PROPERTIES_ACTION_CLASSNAME, "com.l7tech.console.action.CookieCredentialSourceAssertionPropertiesAction");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "HTTP Cookie Properties");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_ICON, "com/l7tech/console/resources/Properties16.gif");

        return meta;

    }
}
