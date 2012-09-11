package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.RequireWssSaml;
import com.l7tech.policy.assertion.xmlsec.SecurityHeaderAddressableSupport;
import com.l7tech.security.token.SamlSecurityToken;
import com.l7tech.security.token.XmlSecurityToken;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.util.WSSecurityProcessorUtils;
import com.l7tech.util.Triple;
import org.jetbrains.annotations.NotNull;
import org.xml.sax.SAXException;

import java.io.IOException;

/**
 * Require a SAML token delivered via WS-Security.
 *
 * @param <AT> RequireWssSaml
 */
public class ServerRequireWssSaml<AT extends RequireWssSaml> extends ServerRequireSaml<AT> {

    public ServerRequireWssSaml(AT sa) {
        super(sa);
    }

    /**
     * Validates the SAML assertion is targeted at the Gateway before passing to super class.
     *
     * @param context processing context
     * @return AssertionStatus.NONE if this Assertion did its business successfully; otherwise, some error code
     * @throws com.l7tech.policy.assertion.PolicyAssertionException
     *          something is wrong in the policy dont throw this if there is an issue with the request or the response
     */
    @Override
    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        if (!SecurityHeaderAddressableSupport.isLocalRecipient(assertion)) {
            logAndAudit(AssertionMessages.REQUESTWSS_NOT_FOR_US);
            return AssertionStatus.NONE;
        }

        return super.checkRequest( context );
    }

    // - PROTECTED

    @NotNull
    @Override
    protected Triple<AssertionStatus, ProcessorResult, SamlSecurityToken> getSamlSecurityTokenAndContext(Message message, String messageDesc, AuthenticationContext authContext) throws IOException {

        try {
            if (!message.isSoap()) {
                logAndAudit(AssertionMessages.SAML_AUTHN_STMT_REQUEST_NOT_SOAP, messageDesc);
                return new Triple<AssertionStatus, ProcessorResult, SamlSecurityToken>(AssertionStatus.NOT_APPLICABLE, null, null);
            }
        } catch (SAXException e) {
            throw new IOException(e);
        }

        ProcessorResult wssResults;
        if ( isRequest() && !config.getBooleanProperty( ServerConfigParams.PARAM_WSS_PROCESSOR_LAZY_REQUEST,true) ) {
            wssResults = message.getSecurityKnob().getProcessorResult();
        } else {
            wssResults = WSSecurityProcessorUtils.getWssResults(message, messageDesc, securityTokenResolver, getAudit());
        }

        if (wssResults == null) {
            logAndAudit(AssertionMessages.SAML_AUTHN_STMT_NO_TOKENS_PROCESSED, messageDesc);
            if (isRequest())
                authContext.setAuthenticationMissing();
            return new Triple<AssertionStatus, ProcessorResult, SamlSecurityToken>(AssertionStatus.AUTH_REQUIRED, null, null);
        }

        XmlSecurityToken[] tokens = wssResults.getXmlSecurityTokens();
        if (tokens == null) {
            logAndAudit(AssertionMessages.SAML_AUTHN_STMT_NO_TOKENS_PROCESSED, messageDesc);
            if (isRequest())
                authContext.setAuthenticationMissing();
            return new Triple<AssertionStatus, ProcessorResult, SamlSecurityToken>(AssertionStatus.AUTH_REQUIRED, null, null);
        }
        SamlSecurityToken samlAssertion = null;
        for (XmlSecurityToken tok : tokens) {
            if (tok instanceof SamlSecurityToken) {
                SamlSecurityToken samlToken = (SamlSecurityToken) tok;
                if (samlAssertion != null) {
                    logAndAudit(AssertionMessages.SAML_AUTHN_STMT_MULTIPLE_SAML_ASSERTIONS_UNSUPPORTED, messageDesc);
                    final AssertionStatus badMessageStatus = getBadMessageStatus();
                    return new Triple<AssertionStatus, ProcessorResult, SamlSecurityToken>(badMessageStatus, null, null);
                }
                samlAssertion = samlToken;
            }
        }

        return new Triple<AssertionStatus, ProcessorResult, SamlSecurityToken>(AssertionStatus.NONE, wssResults, samlAssertion);
    }

}
