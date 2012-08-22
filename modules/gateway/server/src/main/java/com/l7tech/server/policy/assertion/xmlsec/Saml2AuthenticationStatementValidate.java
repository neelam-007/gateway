package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.policy.assertion.xmlsec.RequireWssSaml;
import com.l7tech.policy.assertion.xmlsec.SamlAuthenticationStatement;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.util.ArrayUtils;
import com.l7tech.util.Pair;
import org.apache.xmlbeans.XmlObject;
import x0Assertion.oasisNamesTcSAML2.AuthnContextType;
import x0Assertion.oasisNamesTcSAML2.AuthnStatementType;

import java.util.*;

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
     * @param statementAbstractType
     * @param wssResults
     * @param validationResults where the results are collected
     * @param collectAttrValues
     * @param serverVariables
     * @param auditor
     */
    @Override
    protected void validate(XmlObject statementAbstractType,
                            ProcessorResult wssResults,
                            Collection<SamlAssertionValidate.Error> validationResults,
                            Collection<Pair<String, String[]>> collectAttrValues,
                            Map<String, Object> serverVariables,
                            Audit auditor) {
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

        final String customAuthMethods = authenticationStatementConstraints.getCustomAuthenticationMethods();
        validateAuthenticationMethods(authenticationMethod, Arrays.asList(methods),
                customAuthMethods,
                validationResults,
                serverVariables,
                auditor);
    }
}
