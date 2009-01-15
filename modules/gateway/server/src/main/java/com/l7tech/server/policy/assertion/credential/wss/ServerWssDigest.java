package com.l7tech.server.policy.assertion.credential.wss;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.wss.WssDigest;
import com.l7tech.security.token.UsernameToken;
import com.l7tech.security.token.UsernameTokenImpl;
import com.l7tech.security.token.XmlSecurityToken;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.audit.LogOnlyAuditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 */
public class ServerWssDigest extends AbstractServerAssertion<WssDigest> {
    private static final Logger logger = Logger.getLogger(ServerWssDigest.class.getName());

    final Audit auditor;

    public ServerWssDigest(WssDigest assertion, ApplicationContext context) {
        super(assertion);
        this.auditor = context == null ? new LogOnlyAuditor(logger) : new Auditor(this, context, logger);
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        Message req = context.getRequest();
        try {
            if (!req.isSoap()) {
                auditor.logAndAudit(AssertionMessages.REQUEST_NOT_SOAP);
                return AssertionStatus.NOT_APPLICABLE;
            }

            ProcessorResult pr = req.getSecurityKnob().getProcessorResult();
            if (pr == null) {
                auditor.logAndAudit(AssertionMessages.REQUESTWSS_NO_SECURITY);
                context.setAuthenticationMissing();
                context.setRequestPolicyViolated();
                return AssertionStatus.AUTH_REQUIRED;
            }

            Collection<UsernameToken> utoks = getUsernameTokens(pr);
            for (UsernameToken utok : utoks) {
                AssertionStatus ret = checkToken(utok);
                if (ret == AssertionStatus.NONE) {
                    auditor.logAndAudit(AssertionMessages.USERDETAIL_INFO, "WSS Digest token validated successfully");
                    return ret;
                }
            }

            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "No conforming WSS Digest token was found in request");
            context.setRequestPolicyViolated();
            return AssertionStatus.FALSIFIED;
        } catch (SAXException e) {
            auditor.logAndAudit(AssertionMessages.REQUEST_BAD_XML);
            return AssertionStatus.BAD_REQUEST;
        }
    }

    private AssertionStatus checkToken(UsernameToken utok) {
        if (!utok.isDigest())
            return falsifyFine("Ignoring UsernameToken that does not contain a digest password");

        String digest = utok.getPasswordDigest();
        String nonce = utok.getNonce();
        String created = utok.getCreated();

        if (assertion.isRequireTimestamp()) {
            if (created == null)
                return falsify("Ignoring UsernameToken that does not contain a timestamp");
            // TODO check for stale timestamp
        }

        if (assertion.isRequireNonce()) {
            if (nonce == null)
                return falsify("Ignoring UsernameToken that does not contain a nonce");
            // TODO check for recently-seen nonces; can throw out remembered nonces with old timestamps
        }

        final String requiredPass = assertion.getRequiredPassword();
        if (requiredPass == null)
            throw new AssertionStatusException(AssertionStatus.SERVER_ERROR, "WssDigest assertion has no password configured");
        String expectedDigest = UsernameTokenImpl.createPasswordDigest(requiredPass.toCharArray(), created, nonce);

        if (!expectedDigest.equals(digest))
            return falsify("UsernameToken digest value does not match the expected value");

        // We're good
        return AssertionStatus.NONE;
    }

    private AssertionStatus falsifyFine(String msg) {
        auditor.logAndAudit(AssertionMessages.USERDETAIL_FINE, msg);
        return AssertionStatus.FALSIFIED;
    }

    private AssertionStatus falsify(String msg) {
        auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, msg);
        return AssertionStatus.FALSIFIED;
    }

    private Collection<UsernameToken> getUsernameTokens(ProcessorResult pr) {
        List<UsernameToken> ret = new ArrayList<UsernameToken>();

        for (XmlSecurityToken token : pr.getXmlSecurityTokens()) {
            if (token instanceof UsernameToken) {
                UsernameToken utok = (UsernameToken) token;
                ret.add(utok);
            }
        }

        return ret;
    }
}
