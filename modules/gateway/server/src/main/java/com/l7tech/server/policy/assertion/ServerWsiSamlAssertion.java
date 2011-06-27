package com.l7tech.server.policy.assertion;

import java.io.InputStream;
import java.util.logging.Logger;
import javax.xml.xpath.XPathException;

import com.l7tech.policy.assertion.WsiSamlAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;

/**
 * Server assertion for WSI-SAML Token Profile compliance.
 */
public class ServerWsiSamlAssertion extends ServerXpathValidationAssertion<WsiSamlAssertion> {

    //- PUBLIC

    /**
     * Server assertion for WSI-SAML Token Profile compliance.
     *
     * @param wsiSamlAssertion assertion data object
     */
    public ServerWsiSamlAssertion(WsiSamlAssertion wsiSamlAssertion) {
        super(wsiSamlAssertion);
    }

    //- PROTECTED

    @Override
    protected boolean isCheckRequestMessages() {
        return assertion.isCheckRequestMessages();
    }

    @Override
    protected boolean isCheckResponseMessages() {
        return assertion.isCheckResponseMessages();
    }

    @Override
    protected boolean isFailOnNonCompliantRequest() {
        return assertion.isFailOnNonCompliantRequest();
    }

    @Override
    protected boolean isFailOnNonCompliantResponse() {
        return assertion.isFailOnNonCompliantResponse();
    }

    @Override
    protected void onRequestFailure() {
        logAndAudit(AssertionMessages.WSI_SAML_REQUEST_FAIL);
    }

    @Override
    protected void onRequestNonCompliance(String ruleId, String description) {
        if(assertion.isAuditRequestNonCompliance())
            logAndAudit(AssertionMessages.WSI_SAML_REQUEST_NON_COMPLIANT, ruleId, description);
    }

    @Override
    protected void onRequestNonSoap() {
        logAndAudit(AssertionMessages.WSI_SAML_REQUEST_NON_SOAP);
    }

    @Override
    protected void onResponseFailure() {
        logAndAudit(AssertionMessages.WSI_SAML_RESPONSE_FAIL);
    }

    @Override
    protected void onResponseNonCompliance(String ruleId, String description) {
        if(assertion.isAuditResponseNonCompliance())
            logAndAudit(AssertionMessages.WSI_SAML_RESPONSE_NON_COMPLIANT, ruleId, description);
    }

    @Override
    protected void onResponseNonSoap() {
        logAndAudit(AssertionMessages.WSI_SAML_RESPONSE_NON_SOAP);
    }

    @Override
    protected void onXPathError(XPathException xpe) {
        logAndAudit(AssertionMessages.WSI_SAML_XPATH_ERROR, null, xpe);
    }

    /**
     * Return input stream from which properties can be read
     */
    @Override
    protected InputStream getRulesResource() {
        ClassLoader loader = ServerWsiSamlAssertion.class.getClassLoader();
        return loader.getResourceAsStream(ServerWsiSamlAssertion.RESOURCE_RULES);
    }

    //- PRIVATE

    //
    private static final String RESOURCE_RULES = "com/l7tech/server/policy/assertion/ServerWsiSamlAssertion.rules.properties";
}
