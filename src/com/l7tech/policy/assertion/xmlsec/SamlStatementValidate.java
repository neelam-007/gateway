/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.policy.assertion.xmlsec;

import org.w3c.dom.Document;

import java.util.Collection;
import java.util.ArrayList;

/**
 * Validates the SAML Assertion within the Document. The Document must represent a well formed
 * SOAP message.
 *
 * @author emil
 * @version Jan 25, 2005
 */
public abstract class SamlStatementValidate {
    protected Collection errorCollector = new ArrayList();
    protected final SamlStatementAssertion statementAssertion;

    public static SamlStatementValidate getValidate(SamlStatementAssertion sa) {
        if (sa == null) {
            throw new IllegalArgumentException("Non Null Saml Statement Assertion required");
        }
        if (sa instanceof SamlAuthenticationStatement) {

        }
        return null;
    }

    protected SamlStatementValidate(SamlStatementAssertion statementAssertion) {
        this.statementAssertion = statementAssertion;
    }

    /**
     * Sets a collection object for collecting Saml errors during parsing, validation
     * @param errorCollector
     */
    public void setErrorCollector(Collection errorCollector) {
        this.errorCollector = errorCollector;
    }

    /**
     * Validate the Document. Containing the Saml assertion
     * @param document
     */
    public void validate(Document document) {

    }

    /**
     * Valudates the Saml
     */
    protected void validateSamlAssertionStructure() {

    }

    /**
     * Validate the statement
     */
    protected abstract void validateStatement();
}