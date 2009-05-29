package com.l7tech.server.policy.assertion;

import java.io.InputStream;
import java.util.logging.Logger;
import javax.xml.xpath.XPathException;

import org.springframework.context.ApplicationContext;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.server.audit.Auditor;
import com.l7tech.policy.assertion.WsiBspAssertion;

/**
 * Server assertion for WSI-BSP compliance.
 *
 * @author $Author$
 * @version $Revision$
 */
public class ServerWsiBspAssertion extends ServerXpathValidationAssertion<WsiBspAssertion> {

    //- PUBLIC

    /**
     * Server assertion for WSI-BSP compliance.
     *
     * @param wsiBspAssertion assertion data object
     * @param springContext the application context to use
     */
    public ServerWsiBspAssertion(WsiBspAssertion wsiBspAssertion, ApplicationContext springContext) {
        super(wsiBspAssertion, logger);
        this.auditor = (springContext!=null) ? new Auditor(this, springContext, logger) : null;
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
        auditor.logAndAudit(AssertionMessages.WSI_BSP_REQUEST_FAIL);
    }

    @Override
    protected void onRequestNonCompliance(String ruleId, String description) {
        if(assertion.isAuditRequestNonCompliance())
            auditor.logAndAudit(AssertionMessages.WSI_BSP_REQUEST_NON_COMPLIANT, ruleId,description);
    }

    @Override
    protected void onRequestNonSoap() {
        auditor.logAndAudit(AssertionMessages.WSI_BSP_REQUEST_NON_SOAP);
    }

    @Override
    protected void onResponseFailure() {
        auditor.logAndAudit(AssertionMessages.WSI_BSP_RESPONSE_FAIL);
    }

    @Override
    protected void onResponseNonCompliance(String ruleId, String description) {
        if(assertion.isAuditResponseNonCompliance())
            auditor.logAndAudit(AssertionMessages.WSI_BSP_RESPONSE_NON_COMPLIANT, ruleId,description);
    }

    @Override
    protected void onResponseNonSoap() {
        auditor.logAndAudit(AssertionMessages.WSI_BSP_RESPONSE_NON_SOAP);
    }

    @Override
    protected void onXPathError(XPathException xpe) {
        auditor.logAndAudit(AssertionMessages.WSI_BSP_XPATH_ERROR, null, xpe);
    }

    /**
     * Return input stream from which properties can be read
     */
    @Override
    protected InputStream getRulesResource() {
        ClassLoader loader = ServerWsiBspAssertion.class.getClassLoader();
        return loader.getResourceAsStream(RESOURCE_RULES);
    }

    //- PACKAGE

    /**
     * Used by test code only (tests rule loading).
     */
    ServerWsiBspAssertion() {
        super(null, logger);
        this.auditor = null;
    }

    //- PRIVATE

    //
    private static final Logger logger = Logger.getLogger(ServerWsiBspAssertion.class.getName());
    private static final String RESOURCE_RULES = "com/l7tech/server/policy/assertion/ServerWsiBspAssertion.rules.properties";

    //
    private final Auditor auditor;
}
