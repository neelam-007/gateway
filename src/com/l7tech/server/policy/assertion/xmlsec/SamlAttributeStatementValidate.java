package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.security.xml.processor.ProcessorResult;
import com.l7tech.policy.assertion.xmlsec.SamlStatementAssertion;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import x0Assertion.oasisNamesTcSAML1.SubjectStatementAbstractType;

import java.util.Collection;

/**
 * @author emil
 * @version 27-Jan-2005
 */
class SamlAttributeStatementValidate extends SamlStatementValidate {
    /**
     * Construct  the <code>SamlStatementValidate</code> for the statement assertion
     *
     * @param statementAssertion the saml statemenet assertion
     * @param applicationContext the applicaiton context to allo access to components and services
     */
    SamlAttributeStatementValidate(SamlStatementAssertion statementAssertion, ApplicationContext applicationContext) {
        super(statementAssertion, applicationContext);
    }

    /**
     * Validate the attribute statement
     * @param document
     * @param statementAbstractType
     * @param wssResults
     * @param validationResults where the results are collected
     */
    protected void validateStatement(Document document,
                                     SubjectStatementAbstractType statementAbstractType,
                                     ProcessorResult wssResults, Collection validationResults) {
        throw new RuntimeException("Not yet implemented");
    }

}
