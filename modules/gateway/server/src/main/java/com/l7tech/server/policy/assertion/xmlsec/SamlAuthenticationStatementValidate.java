package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.policy.assertion.xmlsec.RequireSaml;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.policy.assertion.xmlsec.SamlAuthenticationStatement;
import com.l7tech.util.Pair;
import org.apache.xmlbeans.XmlObject;
import x0Assertion.oasisNamesTcSAML1.AuthenticationStatementType;

import java.util.Collection;
import java.util.Arrays;
import java.util.Map;

/**
 * @author emil
 * @version 27-Jan-2005
 */
class SamlAuthenticationStatementValidate extends SamlStatementValidate {
    private SamlAuthenticationStatement authenticationStatementConstraints;

    /**
     * Construct  the <code>SamlAssertionValidate</code> for the statement assertion
     *
     * @param requestSaml the saml statement assertion
     */
    SamlAuthenticationStatementValidate(RequireSaml requestSaml) {
        super(requestSaml);
        authenticationStatementConstraints = requestSaml.getAuthenticationStatement();
        if (authenticationStatementConstraints == null) {
            throw new IllegalArgumentException("Authentication requirements have not been specified");
        }
    }

    /**
     * Validate the authentication statement
     * @param statementAbstractType
     * @param validationResults where the results are collected
     * @param collectAttrValues
     * @param serverVariables
     * @param auditor
     */
    @Override
    protected void validate(XmlObject statementAbstractType,
                            Collection<SamlAssertionValidate.Error> validationResults,
                            Collection<Pair<String, String[]>> collectAttrValues,
                            Map<String, Object> serverVariables,
                            Audit auditor) {
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

        final String customAuthMethods = authenticationStatementConstraints.getCustomAuthenticationMethods();
        validateAuthenticationMethods(authenticationMethod, Arrays.asList(methods),
                customAuthMethods,
                validationResults,
                serverVariables,
                auditor);
    }

    /**
     * Filter out any name formats that are not allowed in v1
     */
    private String[] filterAuthenticationMethods(String[] authenticationMethods) {
        return SamlAssertionValidate.filter(authenticationMethods, SamlConstants.ALL_AUTHENTICATIONS);
    }
}
