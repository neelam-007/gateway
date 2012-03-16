/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.assertion.credential.http;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;
import org.apache.commons.lang.StringUtils;

/**
 * A credential source assertion that gathers from an HTTP cookie.
 */
public class CookieCredentialSourceAssertion extends Assertion implements SetsVariables {
    public static final String DEFAULT_COOKIE_NAME = "session";
    public static final String DEFAULT_VARIABLE_PREFIX = "cookie";
    public String cookieName = DEFAULT_COOKIE_NAME;
    private String variablePrefix = DEFAULT_VARIABLE_PREFIX;

    public String getCookieName() {
        return cookieName;
    }

    public void setCookieName(String cookieName) {
        if (cookieName == null || cookieName.length() < 1)
            throw new IllegalArgumentException("Cookie name must be non-empty.");
        this.cookieName = cookieName;
    }

    public String getVariablePrefix() {
        return variablePrefix;
    }

    public void setVariablePrefix(final String variablePrefix) {
        this.variablePrefix = variablePrefix;
    }

    @Override
    public boolean isCredentialSource() {
        return true;
    }

    final static String baseName = "Require HTTP Cookie";

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<CookieCredentialSourceAssertion>() {
        @Override
        public String getAssertionName(final CookieCredentialSourceAssertion assertion, final boolean decorate) {
            if (!decorate) return baseName;

            return baseName + " (name=" + assertion.getCookieName() + ")";
        }
    };

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"accessControl"});

        meta.put(AssertionMetadata.SHORT_NAME, baseName);
        meta.put(AssertionMetadata.DESCRIPTION, "Gather credentials in the form of an HTTP session cookie.");
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/authentication.gif");
        meta.put(AssertionMetadata.USED_BY_CLIENT, Boolean.TRUE);
        meta.put(AssertionMetadata.CLIENT_ASSERTION_POLICY_ICON, "com/l7tech/proxy/resources/tree/authentication.gif");

        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, policyNameFactory);

        meta.put(AssertionMetadata.PROPERTIES_ACTION_CLASSNAME, "com.l7tech.console.action.CookieCredentialSourceAssertionPropertiesAction");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "HTTP Cookie Properties");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_ICON, "com/l7tech/console/resources/Properties16.gif");

        return meta;

    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        if (StringUtils.isBlank(cookieName)) {
            return new VariableMetadata[0];
        } else {
            final String contextVariableName = variablePrefix + "." + cookieName;
            return new VariableMetadata[]{new VariableMetadata(contextVariableName, false, false, contextVariableName, false, DataType.STRING)};
        }
    }
}
