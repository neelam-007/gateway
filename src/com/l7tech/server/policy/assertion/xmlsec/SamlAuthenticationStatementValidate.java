package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.security.xml.processor.ProcessorResult;
import com.l7tech.policy.assertion.xmlsec.SamlStatementAssertion;
import com.l7tech.policy.assertion.xmlsec.SamlAuthenticationStatement;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import x0Assertion.oasisNamesTcSAML1.AuthenticationStatementType;
import x0Assertion.oasisNamesTcSAML1.SubjectStatementAbstractType;

import java.util.Collection;

/**
 * @author emil
 * @version 27-Jan-2005
 */
class SamlAuthenticationStatementValidate extends SamlStatementValidate {
    /**
     * Construct  the <code>SamlStatementValidate</code> for the statement assertion
     *
     * @param statementAssertion the saml statement assertion
     * @param applicationContext the application context to allow access to components and services
     */
    SamlAuthenticationStatementValidate(SamlStatementAssertion statementAssertion, ApplicationContext applicationContext) {
        super(statementAssertion, applicationContext);
    }

    /**
     * Validate the authentication statement
     * @param document
     * @param statementAbstractType
     * @param wssResults
     * @param validationResults where the results are collected
     */
    protected void validateStatement(Document document,
                                     SubjectStatementAbstractType statementAbstractType,
                                     ProcessorResult wssResults, Collection validationResults) {
        if (!(statementAbstractType instanceof AuthenticationStatementType)) {
            throw new IllegalArgumentException("Expected "+AuthenticationStatementType.class);
        }
        AuthenticationStatementType authenticationStatementType = (AuthenticationStatementType)statementAbstractType;
        String authenticationMethod = authenticationStatementType.getAuthenticationMethod();
        if (authenticationMethod == null) {
            validationResults.add(new Error("No Authentication Method specified", authenticationStatementType.toString(), null, null));
            return;
        }
        SamlAuthenticationStatement assertionConstraints = (SamlAuthenticationStatement)statementAssertionConstraints;
        String[] methods = assertionConstraints.getAuthenticationMethods();
        boolean methodMatches = methods.length == 0;
        for (int i = 0; i < methods.length; i++) {
            String method = methods[i];
            if (authenticationMethod.equals(method)) {
                methodMatches = true;
                logger.finer("Matched authentication method "+method);
                break;
            }
        }
        if (!methodMatches) {
            final String msg = "Authentication method " + authenticationMethod + " not matched";
            validationResults.add(new Error(msg, authenticationStatementType.toString(), methods, null));
            logger.finer(msg);
        }
    }
}
