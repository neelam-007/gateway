/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.policy.assertion.xmlsec.RequireWssSaml;
import org.w3c.dom.Document;
import org.apache.xmlbeans.XmlObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.TimeZone;
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
    protected final RequireWssSaml requestWssSaml;


    /**
     * Construct  the <code>SamlAssertionValidate</code> for the statement assertion
     *
     * @param requestWssSaml the saml assertion that specifies constraints
     */
    public SamlStatementValidate(RequireWssSaml requestWssSaml) {
        this.requestWssSaml = requestWssSaml;
    }


    /**
     * Validate the specific <code>SubjectStatementAbstractType</code> and collect eventual validation
     * errors in the validationResults collection.
     *
     * @param document              the message document
     * @param statementObject       the subject statement type, that may be authentication statement
     *                              authorization statement or attribute statement
     * @param wssResults            the wssresults collection
     * @param validationResults     where the valida
     */
    protected abstract void validate(Document document,
                                     XmlObject statementObject,
                                     ProcessorResult wssResults, Collection validationResults);

}