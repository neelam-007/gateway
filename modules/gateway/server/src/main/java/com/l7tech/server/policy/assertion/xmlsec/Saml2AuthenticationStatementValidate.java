package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.xmlsec.RequireWssSaml;
import com.l7tech.policy.assertion.xmlsec.SamlAuthenticationStatement;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.util.ArrayUtils;
import org.apache.xmlbeans.XmlObject;
import org.w3c.dom.Document;
import x0Assertion.oasisNamesTcSAML2.AuthnContextType;
import x0Assertion.oasisNamesTcSAML2.AuthnStatementType;

import java.util.Arrays;
import java.util.Collection;

/**
 * Validation for SAML 2.x Authentication statement.
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
class Saml2AuthenticationStatementValidate extends SamlStatementValidate {
    private SamlAuthenticationStatement authenticationStatementConstraints;

    /**
     * Construct  the <code>SamlAssertionValidate</code> for the statement assertion
     *
     * @param requestWssSaml the saml statement assertion
     */
    Saml2AuthenticationStatementValidate(RequireWssSaml requestWssSaml) {
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
        if (!(statementAbstractType instanceof AuthnStatementType)) {
            throw new IllegalArgumentException("Expected "+AuthnStatementType.class);
        }
        AuthnStatementType authenticationStatementType = (AuthnStatementType)statementAbstractType;
        AuthnContextType authnContext = authenticationStatementType.getAuthnContext();
        String authenticationMethod = null;
        if (authnContext != null) {
            authenticationMethod = authnContext.getAuthnContextClassRef();
        }
        if (authenticationMethod == null) {
            SamlAssertionValidate.Error result = new SamlAssertionValidate.Error("No Authentication Method specified", null);
            validationResults.add(result);
            logger.finer(result.toString());
            return;
        }
        String[] methods = ArrayUtils.copy(authenticationStatementConstraints.getAuthenticationMethods());
        for (int i = 0; i < methods.length; i++) {
            String method = methods[i];
            String v2method = (String) SamlConstants.AUTH_MAP_SAML_1TO2.get(method);
            if (v2method != null) {
                methods[i] = v2method;
            }
        }

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
            validationResults.add(new SamlAssertionValidate.Error(msg, null,
                                                                  methods.length == 1 ? methods[0]
                                                                                : Arrays.asList(methods).toString(), authenticationMethod));
            logger.finer(msg);
        }
    }
}
