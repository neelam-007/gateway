package com.l7tech.server.policy.assertion;

import java.io.InputStream;
import java.util.logging.Logger;
import javax.xml.xpath.XPathException;

import org.springframework.context.ApplicationContext;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.server.audit.Auditor;
import com.l7tech.policy.assertion.WsiBspAssertion;

/**
 * Server assertion for WSI-BSP compliance.
 *
 * @author $Author$
 * @version $Revision$
 */
public class ServerWsiBspAssertion extends ServerXpathValidationAssertion {

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
        this.wsiBspAssertion = wsiBspAssertion;
    }

    //- PROTECTED

    protected boolean isCheckRequestMessages() {
        return wsiBspAssertion.isCheckRequestMessages();
    }

    protected boolean isCheckResponseMessages() {
        return wsiBspAssertion.isCheckResponseMessages();
    }

    protected boolean isFailOnNonCompliantRequest() {
        return wsiBspAssertion.isFailOnNonCompliantRequest();
    }

    protected boolean isFailOnNonCompliantResponse() {
        return wsiBspAssertion.isFailOnNonCompliantResponse();
    }

    protected void onRequestFailure() {
        auditor.logAndAudit(AssertionMessages.WSI_BSP_REQUEST_FAIL);
    }

    protected void onRequestNonCompliance(String ruleId, String description) {
        if(wsiBspAssertion.isAuditRequestNonCompliance())
            auditor.logAndAudit(AssertionMessages.WSI_BSP_REQUEST_NON_COMPLIANT, new String[]{ruleId,description});
    }

    protected void onRequestNonSoap() {
        auditor.logAndAudit(AssertionMessages.WSI_BSP_REQUEST_NON_SOAP);
    }

    protected void onResponseFailure() {
        auditor.logAndAudit(AssertionMessages.WSI_BSP_RESPONSE_FAIL);
    }

    protected void onResponseNonCompliance(String ruleId, String description) {
        if(wsiBspAssertion.isAuditResponseNonCompliance())
            auditor.logAndAudit(AssertionMessages.WSI_BSP_RESPONSE_NON_COMPLIANT, new String[]{ruleId,description});
    }

    protected void onResponseNonSoap() {
        auditor.logAndAudit(AssertionMessages.WSI_BSP_RESPONSE_NON_SOAP);
    }

    protected void onXPathError(XPathException xpe) {
        auditor.logAndAudit(AssertionMessages.WSI_BSP_XPATH_ERROR, null, xpe);
    }

    /**
     * Return input stream from which properties can be read
     */
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
        this.wsiBspAssertion = null;
    }

    //- PRIVATE

    //
    private static final Logger logger = Logger.getLogger(ServerWsiBspAssertion.class.getName());
    private static final String RESOURCE_RULES = "com/l7tech/server/policy/assertion/ServerWsiBspAssertion.rules.properties";

    //
    private final Auditor auditor;
    private final WsiBspAssertion wsiBspAssertion;
}
