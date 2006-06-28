package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.security.xml.processor.ProcessorResult;
import com.l7tech.common.security.saml.SamlConstants;
import com.l7tech.policy.assertion.xmlsec.RequestWssSaml;
import com.l7tech.policy.assertion.xmlsec.SamlAuthenticationStatement;
import org.w3c.dom.Document;
import org.apache.xmlbeans.XmlObject;
import x0Assertion.oasisNamesTcSAML1.AuthenticationStatementType;

import java.util.Collection;
import java.util.Arrays;

/**
 * @author emil
 * @version 27-Jan-2005
 */
class SamlAuthenticationStatementValidate extends SamlStatementValidate {
    private SamlAuthenticationStatement authenticationStatementConstraints;

    /**
     * Construct  the <code>SamlAssertionValidate</code> for the statement assertion
     *
     * @param requestWssSaml the saml statement assertion
     */
    SamlAuthenticationStatementValidate(RequestWssSaml requestWssSaml) {
        super(requestWssSaml);
        authenticationStatementConstraints = requestWssSaml.getAuthenticationStatement();
        if (authenticationStatementConstraints == null) {
            throw new IllegalArgumentException("Authentication requirements have not been specified");
        }
    }

    /**
     * Validate the authentication statement
     * @param document
     * @param statementAbstractType
     * @param wssResults
     * @param validationResults where the results are collected
     */
    protected void validate(Document document,
                            XmlObject statementAbstractType,
                            ProcessorResult wssResults, Collection validationResults) {
        if (!(statementAbstractType instanceof AuthenticationStatementType)) {
            throw new IllegalArgumentException("Expected "+AuthenticationStatementType.class);
        }
        AuthenticationStatementType authenticationStatementType = (AuthenticationStatementType)statementAbstractType;
        String authenticationMethod = authenticationStatementType.getAuthenticationMethod();
        if (authenticationMethod == null) {
            SamlAssertionValidate.Error result = new SamlAssertionValidate.Error("No Authentication Method specified", authenticationStatementType.toString(), null, null);
            validationResults.add(result);
            logger.finer(result.toString());
            return;
        }
        String[] methods = filterAuthenticationMethods(authenticationStatementConstraints.getAuthenticationMethods());
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
            final String msg = "Authentication method not matched expected/received: {0}/{1}";
            validationResults.add(new SamlAssertionValidate.Error(msg, authenticationStatementType.toString(),
                                                                  new Object[] {methods.length == 1 ? methods[0].toString()
                                                                                : Arrays.asList(methods).toString(), authenticationMethod}, null));
            logger.finer(msg);
        }
    }

    /**
     * Filter out any name formats that are not allowed in v1
     */
    private String[] filterAuthenticationMethods(String[] authenticationMethods) {
        return SamlAssertionValidate.filter(authenticationMethods, SamlConstants.ALL_AUTHENTICATIONS);
    }
}
