package com.l7tech.external.assertions.samlpassertion;

import java.io.Serializable;

/**
 * The <code>SamlAuthorizationStatementAssertion</code> assertion describes
 * the SAML Authorization Statement constraints.
 */
public class SamlpAuthorizationStatement implements Cloneable, Serializable {
    private static final long serialVersionUID = 1L;

    private String resource;
    private String[] actions; // includes namespace
//    private String action;
//    private String actionNamespace;

    public String[] getActions() {
        if (actions == null)
            return new String[0];
        return actions;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public void setActions(String[] actions) {
        this.actions = actions;
    }

    public Object clone() {
        try {

            SamlpAuthorizationStatement stmt = (SamlpAuthorizationStatement) super.clone();
            return stmt;
        }
        catch(CloneNotSupportedException cnse) {
            throw new RuntimeException("Clone error");
        }
    }
}
