package com.l7tech.server.policy.assertion;

import java.io.InputStream;
import javax.xml.xpath.XPathException;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.policy.assertion.WsiBspAssertion;

/**
 * Server assertion for WSI-BSP compliance.
 */
public class ServerWsiBspAssertion extends ServerXpathValidationAssertion<WsiBspAssertion> {

    //- PUBLIC

    /**
     * Server assertion for WSI-BSP compliance.
     *
     * @param wsiBspAssertion assertion data object
     */
    public ServerWsiBspAssertion(WsiBspAssertion wsiBspAssertion) {
        super(wsiBspAssertion);
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
        logAndAudit(AssertionMessages.WSI_BSP_REQUEST_FAIL);
    }

    @Override
    protected void onRequestNonCompliance(String ruleId, String description) {
        if(assertion.isAuditRequestNonCompliance())
            logAndAudit(AssertionMessages.WSI_BSP_REQUEST_NON_COMPLIANT, ruleId,description);
    }

    @Override
    protected void onRequestNonSoap() {
        logAndAudit(AssertionMessages.WSI_BSP_REQUEST_NON_SOAP);
    }

    @Override
    protected void onResponseFailure() {
        logAndAudit(AssertionMessages.WSI_BSP_RESPONSE_FAIL);
    }

    @Override
    protected void onResponseNonCompliance(String ruleId, String description) {
        if(assertion.isAuditResponseNonCompliance())
            logAndAudit(AssertionMessages.WSI_BSP_RESPONSE_NON_COMPLIANT, ruleId,description);
    }

    @Override
    protected void onResponseNonSoap() {
        logAndAudit(AssertionMessages.WSI_BSP_RESPONSE_NON_SOAP);
    }

    @Override
    protected void onXPathError(XPathException xpe) {
        logAndAudit(AssertionMessages.WSI_BSP_XPATH_ERROR, null, xpe);
    }

    /**
     * Return input stream from which properties can be read
     */
    @Override
    protected InputStream getRulesResource() {
        ClassLoader loader = ServerWsiBspAssertion.class.getClassLoader();
        return loader.getResourceAsStream(RESOURCE_RULES);
    }

    //- PRIVATE

    //
    private static final String RESOURCE_RULES = "com/l7tech/server/policy/assertion/ServerWsiBspAssertion.rules.properties";
}
