package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.policy.assertion.xmlsec.RequireWssSaml;
import com.l7tech.policy.assertion.xmlsec.SamlAuthenticationStatement;
import com.l7tech.util.Pair;
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
    SamlAuthenticationStatementValidate(RequireWssSaml requestWssSaml) {
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
     * @param collectAttrValues
     */
    protected void validate(Document document,
                            XmlObject statementAbstractType,
                            ProcessorResult wssResults, Collection validationResults, Collection<Pair<String, String[]>> collectAttrValues) {
        if (!(statementAbstractType instanceof AuthenticationStatementType)) {
            throw new IllegalArgumentException("Expected "+AuthenticationStatementType.class);
        }
        AuthenticationStatementType authenticationStatementType = (AuthenticationStatementType)statementAbstractType;
        String authenticationMethod = authenticationStatementType.getAuthenticationMethod();
        if (authenticationMethod == null) {
            SamlAssertionValidate.Error result = new SamlAssertionValidate.Error("No Authentication Method specified", null);
            validationResults.add(result);
            logger.finer(result.toString());
            return;
        }
        String[] methods = filterAuthenticationMethods(authenticationStatementConstraints.getAuthenticationMethods());
        boolean methodMatches = methods.length == 0;
        for (String method : methods) {
            if (authenticationMethod.equals(method)) {
                methodMatches = true;
                logger.finer("Matched authentication method " + method);
                break;
            }
        }
        if (!methodMatches) {
            final String msg = "Authentication method not matched expected/received: {0}/{1}";
            validationResults.add(new SamlAssertionValidate.Error(msg,
                                                                  null,
                                                                  methods.length == 1 ? methods[0]
                                                                                : Arrays.asList(methods).toString(), authenticationMethod
            ));
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
