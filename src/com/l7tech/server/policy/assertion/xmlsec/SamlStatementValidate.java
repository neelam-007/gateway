/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.security.token.SecurityToken;
import com.l7tech.common.security.token.SecurityTokenType;
import com.l7tech.common.security.xml.processor.ProcessorResult;
import com.l7tech.common.xml.saml.SamlAssertion;
import com.l7tech.policy.assertion.xmlsec.RequestWssSaml;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import x0Assertion.oasisNamesTcSAML1.*;

import java.util.*;
import java.util.logging.Logger;

/**
 * Validates the SAML Assertion within the Document. The Document must represent a well formed
 * SOAP message.
 *
 * @author emil
 * @version Jan 25, 2005
 */
public abstract class SamlStatementValidate {
    protected static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone("UTC");

    protected final Logger logger = Logger.getLogger(getClass().getName());
    protected Collection errorCollector = new ArrayList();
    protected final RequestWssSaml requestWssSaml;
    private ApplicationContext applicationContext;


    /**
     * Construct  the <code>SamlAssertionValidate</code> for the statement assertion
     *
     * @param requestWssSaml the saml assertion that specifies constraints
     * @param applicationContext the application context to allow access to components and services
     */
    public SamlStatementValidate(RequestWssSaml requestWssSaml, ApplicationContext applicationContext) {
        this.requestWssSaml = requestWssSaml;
        this.applicationContext = applicationContext;
    }


    /**
     * Validate the specific <code>SubjectStatementAbstractType</code> and collect eventual validation
     * errors in the validationResults collection.
     *
     * @param document              the message document
     * @param statementAbstractType the subject statement type, that may be authentication statement
     *                              authorization statement or attribute statement
     * @param wssResults            the wssresults collection
     * @param validationResults     where the valida
     */
    protected abstract void validate(Document document,
                                     SubjectStatementAbstractType statementAbstractType,
                                     ProcessorResult wssResults, Collection validationResults);


}