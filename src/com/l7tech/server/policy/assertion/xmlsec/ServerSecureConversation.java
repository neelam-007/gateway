package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.security.token.SecurityContextToken;
import com.l7tech.common.security.token.SecurityToken;
import com.l7tech.common.security.xml.decorator.DecorationRequirements;
import com.l7tech.common.security.xml.processor.ProcessorResult;
import com.l7tech.common.util.CausedIOException;
import com.l7tech.common.audit.Auditor;
import com.l7tech.identity.User;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.SecureConversation;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.secureconversation.SecureConversationContextManager;
import com.l7tech.server.secureconversation.SecureConversationSession;
import com.l7tech.server.AssertionMessages;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The SSG-side processing of the SecureConversation assertion.
 *
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Aug 4, 2004<br/>
 * $Id$<br/>
 */
public class ServerSecureConversation implements ServerAssertion {
    public ServerSecureConversation(SecureConversation assertion) {
        // nothing to remember from the passed assertion
    }
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        ProcessorResult wssResults;

        Auditor auditor = new Auditor(context.getAuditContext(), logger);
        try {
            if (!context.getRequest().isSoap()) {
                auditor.logAndAudit(AssertionMessages.SC_REQUEST_NOT_SOAP);
                return AssertionStatus.NOT_APPLICABLE;
            }
            wssResults = context.getRequest().getXmlKnob().getProcessorResult();
        } catch (SAXException e) {
            throw new CausedIOException(e);
        }

        if (wssResults == null) {
            auditor.logAndAudit(AssertionMessages.SC_NO_WSS_LEVEL_SECURITY);
            context.setAuthenticationMissing();
            context.setRequestPolicyViolated();
            return AssertionStatus.FALSIFIED;
        }

        SecurityToken[] tokens = wssResults.getSecurityTokens();
        for (int i = 0; i < tokens.length; i++) {
            SecurityToken token = tokens[i];
            if (token instanceof SecurityContextToken) {
                SecurityContextToken secConTok = (SecurityContextToken)token;
                if (!secConTok.isPossessionProved()) {
                    auditor.logAndAudit(AssertionMessages.SC_NO_PROOF_OF_POSSESSION);
                    continue;
                }
                String contextId = secConTok.getContextIdentifier();
                SecureConversationSession session = SecureConversationContextManager.getInstance().getSession(contextId);
                if (session == null) {
                    auditor.logAndAudit(AssertionMessages.SC_TOKEN_INVALID);
                    context.setRequestPolicyViolated();
                    return AssertionStatus.AUTH_FAILED;
                }
                User authenticatedUser = session.getUsedBy();
                context.setAuthenticated(true);
                context.setAuthenticatedUser(authenticatedUser);
                context.addDeferredAssertion(this, deferredSecureConversationResponseDecoration(session));
                auditor.logAndAudit(AssertionMessages.SC_SESSION_FOR_USER, new String[] {authenticatedUser.getLogin()});
                return AssertionStatus.NONE;
            }
        }
        auditor.logAndAudit(AssertionMessages.SC_REQUEST_NOT_REFER_TO_SC_TOKEN);
        context.setAuthenticationMissing();
        context.setRequestPolicyViolated();
        return AssertionStatus.AUTH_REQUIRED;
    }

    private final ServerAssertion deferredSecureConversationResponseDecoration(final SecureConversationSession session) {
        return new ServerAssertion() {
            public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException {
                DecorationRequirements wssReq;

                Auditor auditor = new Auditor(context.getAuditContext(), logger);
                try {
                    if (!context.getResponse().isSoap()) {
                        auditor.logAndAudit(AssertionMessages.SC_UNABLE_TO_ATTACH_SC_TOKEN);
                        return AssertionStatus.NOT_APPLICABLE;
                    }
                    wssReq = context.getResponse().getXmlKnob().getOrMakeDecorationRequirements();
                } catch (SAXException e) {
                    throw new CausedIOException(e);
                }
                wssReq.setSignTimestamp();
                wssReq.setSecureConversationSession(new DecorationRequirements.SecureConversationSession() {
                    public String getId() {
                        return session.getIdentifier();
                    }
                    public byte[] getSecretKey() {
                        return session.getSharedSecret().getEncoded();
                    }
                });
                return AssertionStatus.NONE;
            }
        };
    }

    private final Logger logger = Logger.getLogger(getClass().getName());
}
