package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.server.audit.Auditor;
import com.l7tech.security.token.SecurityContextToken;
import com.l7tech.security.token.XmlSecurityToken;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.util.CausedIOException;
import com.l7tech.xml.soap.SoapFaultUtils;
import com.l7tech.message.HttpRequestKnob;
import com.l7tech.xml.SoapFaultLevel;
import com.l7tech.identity.User;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.xmlsec.SecureConversation;
import com.l7tech.policy.assertion.xmlsec.SecurityHeaderAddressableSupport;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.secureconversation.SecureConversationContextManager;
import com.l7tech.server.secureconversation.SecureConversationSession;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * The SSG-side processing of the SecureConversation assertion.
 *
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Aug 4, 2004<br/>
 */
public class ServerSecureConversation extends AbstractServerAssertion<SecureConversation> {
    private final Auditor auditor;

    public ServerSecureConversation(SecureConversation assertion, ApplicationContext springContext) {
        super(assertion);
        // nothing to remember from the passed assertion
        auditor = new Auditor(this, springContext, logger);
    }

    private String getIncomingURL(PolicyEnforcementContext context) {
        HttpRequestKnob hrk = context.getRequest().getKnob(HttpRequestKnob.class);
        if (hrk == null) {
            logger.warning("cannot generate incoming URL");
            return "";
        }
        return hrk.getQueryString() == null ? hrk.getRequestUrl() : hrk.getRequestUrl() + "?" + hrk.getQueryString();
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        if (!SecurityHeaderAddressableSupport.isLocalRecipient(assertion)) {
            auditor.logAndAudit(AssertionMessages.REQUESTWSS_NOT_FOR_US);
            return AssertionStatus.NONE;
        }

        ProcessorResult wssResults;

        try {
            if (!context.getRequest().isSoap()) {
                auditor.logAndAudit(AssertionMessages.SC_REQUEST_NOT_SOAP);
                return AssertionStatus.NOT_APPLICABLE;
            }
            wssResults = context.getRequest().getSecurityKnob().getProcessorResult();
        } catch (SAXException e) {
            throw new CausedIOException(e);
        }

        if (wssResults == null) {
            auditor.logAndAudit(AssertionMessages.REQUESTWSS_NO_SECURITY);
            context.setAuthenticationMissing();
            context.setRequestPolicyViolated();
            return AssertionStatus.FALSIFIED;
        }

        XmlSecurityToken[] tokens = wssResults.getXmlSecurityTokens();
        for (XmlSecurityToken token : tokens) {
            if (token instanceof SecurityContextToken) {
                SecurityContextToken secConTok = (SecurityContextToken) token;
                if (!secConTok.isPossessionProved()) {
                    auditor.logAndAudit(AssertionMessages.SC_NO_PROOF_OF_POSSESSION);
                    continue;
                }
                String contextId = secConTok.getContextIdentifier();
                SecureConversationSession session = SecureConversationContextManager.getInstance().getSession(contextId);
                if (session == null) {
                    auditor.logAndAudit(AssertionMessages.SC_TOKEN_INVALID);
                    context.setRequestPolicyViolated();
                    // here, we must override the soapfault detail in order to send the fault required by the spec
                    SoapFaultLevel cfault = new SoapFaultLevel();
                    cfault.setLevel(SoapFaultLevel.TEMPLATE_FAULT);
                    cfault.setFaultTemplate(SoapFaultUtils.badContextTokenFault(context.getService().getSoapVersion(), getIncomingURL(context)));
                    context.setFaultlevel(cfault);
                    return AssertionStatus.AUTH_FAILED;
                }
                User authenticatedUser = session.getUsedBy();
                AuthenticationContext authContext = context.getAuthenticationContext(context.getRequest());
                LoginCredentials loginCreds = LoginCredentials.makeLoginCredentials(session.getCredentials().getSecurityToken(), SecureConversation.class, secConTok);
                authContext.addCredentials(loginCreds);
                context.addDeferredAssertion(this, deferredSecureConversationResponseDecoration(session));
                auditor.logAndAudit(AssertionMessages.SC_SESSION_FOR_USER, authenticatedUser.getLogin());
                return AssertionStatus.NONE;
            }
        }
        auditor.logAndAudit(AssertionMessages.SC_REQUEST_NOT_REFER_TO_SC_TOKEN);
        context.setAuthenticationMissing();
        context.setRequestPolicyViolated();
        return AssertionStatus.AUTH_REQUIRED;
    }

    private ServerAssertion deferredSecureConversationResponseDecoration(final SecureConversationSession session) {
        return new AbstractServerAssertion<SecureConversation>(assertion) {
            @Override
            public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException {
                DecorationRequirements wssReq;

                try {
                    if (!context.getResponse().isSoap()) {
                        auditor.logAndAudit(AssertionMessages.SC_UNABLE_TO_ATTACH_SC_TOKEN);
                        return AssertionStatus.NOT_APPLICABLE;
                    }
                    wssReq = context.getResponse().getSecurityKnob().getOrMakeDecorationRequirements();
                } catch (SAXException e) {
                    auditor.logAndAudit(AssertionMessages.SC_UNABLE_TO_ATTACH_SC_TOKEN);
                    throw new CausedIOException(e);
                }
                wssReq.setSignTimestamp();
                wssReq.setSecureConversationSession(new DecorationRequirements.SecureConversationSession() {
                    @Override
                    public String getId() {
                        return session.getIdentifier();
                    }
                    @Override
                    public byte[] getSecretKey() {
                        return session.getSharedSecret();
                    }
                    @Override
                    public String getSCNamespace() {
                        return session.getSecConvNamespaceUsed();
                    }
                });
                return AssertionStatus.NONE;
            }
        };
    }

    private final Logger logger = Logger.getLogger(getClass().getName());
}
