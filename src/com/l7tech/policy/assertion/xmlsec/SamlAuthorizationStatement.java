package com.l7tech.policy.assertion.xmlsec;

import java.io.Serializable;

/**
 * The <code>SamlAuthorizationStatementAssertion</code> assertion describes
 * the SAML Authorization Statement constraints.
 */
public class SamlAuthorizationStatement implements Serializable {
    private String resource;
    private String action;
    private String actionNamespace;

    public String getAction() {
        return action;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getActionNamespace() {
        return actionNamespace;
    }

    public void setActionNamespace(String actionNamespace) {
        this.actionNamespace = actionNamespace;
    }
}
