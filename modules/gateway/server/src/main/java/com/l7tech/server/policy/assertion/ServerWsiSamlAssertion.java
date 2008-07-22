package com.l7tech.server.policy.assertion;

import java.io.InputStream;
import java.util.logging.Logger;
import javax.xml.xpath.XPathException;

import org.springframework.context.ApplicationContext;

import com.l7tech.policy.assertion.WsiSamlAssertion;
import com.l7tech.server.audit.Auditor;
import com.l7tech.gateway.common.audit.AssertionMessages;

/**
 * Server assertion for WSI-SAML Token Profile compliance.
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class ServerWsiSamlAssertion extends ServerXpathValidationAssertion {

    //- PUBLIC

    /**
     * Server assertion for WSI-SAML Token Profile compliance.
     *
     * @param wsiSamlAssertion assertion data object
     * @param springContext the application context to use
     */
    public ServerWsiSamlAssertion(WsiSamlAssertion wsiSamlAssertion, ApplicationContext springContext) {
        super(wsiSamlAssertion, logger);
        this.auditor = (springContext!=null) ? new Auditor(this, springContext, ServerWsiSamlAssertion.logger) : null;
        this.wsiSamlAssertion = wsiSamlAssertion;
    }

    //- PROTECTED

    protected boolean isCheckRequestMessages() {
        return wsiSamlAssertion.isCheckRequestMessages();
    }

    protected boolean isCheckResponseMessages() {
        return wsiSamlAssertion.isCheckResponseMessages();
    }

    protected boolean isFailOnNonCompliantRequest() {
        return wsiSamlAssertion.isFailOnNonCompliantRequest();
    }

    protected boolean isFailOnNonCompliantResponse() {
        return wsiSamlAssertion.isFailOnNonCompliantResponse();
    }

    protected void onRequestFailure() {
        auditor.logAndAudit(AssertionMessages.WSI_SAML_REQUEST_FAIL);
    }

    protected void onRequestNonCompliance(String ruleId, String description) {
        if(wsiSamlAssertion.isAuditRequestNonCompliance())
            auditor.logAndAudit(AssertionMessages.WSI_SAML_REQUEST_NON_COMPLIANT, new String[]{ruleId,description});
    }

    protected void onRequestNonSoap() {
        auditor.logAndAudit(AssertionMessages.WSI_SAML_REQUEST_NON_SOAP);
    }

    protected void onResponseFailure() {
        auditor.logAndAudit(AssertionMessages.WSI_SAML_RESPONSE_FAIL);
    }

    protected void onResponseNonCompliance(String ruleId, String description) {
        if(wsiSamlAssertion.isAuditResponseNonCompliance())
            auditor.logAndAudit(AssertionMessages.WSI_SAML_RESPONSE_NON_COMPLIANT, new String[]{ruleId,description});
    }

    protected void onResponseNonSoap() {
        auditor.logAndAudit(AssertionMessages.WSI_SAML_RESPONSE_NON_SOAP);
    }

    protected void onXPathError(XPathException xpe) {
        auditor.logAndAudit(AssertionMessages.WSI_SAML_XPATH_ERROR, null, xpe);
    }

    /**
     * Return input stream from which properties can be read
     */
    protected InputStream getRulesResource() {
        ClassLoader loader = ServerWsiSamlAssertion.class.getClassLoader();
        return loader.getResourceAsStream(ServerWsiSamlAssertion.RESOURCE_RULES);
    }

    //- PACKAGE

    /**
     * Used by test code only (tests rule loading).
     */
    ServerWsiSamlAssertion() {
        super(null, logger);
        this.auditor = null;
        this.wsiSamlAssertion = null;
    }

    //- PRIVATE

    //
    private static final Logger logger = Logger.getLogger(ServerWsiSamlAssertion.class.getName());
    private static final String RESOURCE_RULES = "com/l7tech/server/policy/assertion/ServerWsiSamlAssertion.rules.properties";

    //
    private final Auditor auditor;
    private final WsiSamlAssertion wsiSamlAssertion;
}
