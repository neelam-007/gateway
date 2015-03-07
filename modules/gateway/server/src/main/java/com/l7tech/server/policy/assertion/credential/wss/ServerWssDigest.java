package com.l7tech.server.policy.assertion.credential.wss;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.AuditFactory;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.wss.WssDigest;
import com.l7tech.policy.assertion.xmlsec.SecurityHeaderAddressableSupport;
import com.l7tech.security.token.UsernameToken;
import com.l7tech.security.token.UsernameTokenImpl;
import com.l7tech.security.token.XmlSecurityToken;
import com.l7tech.security.xml.SecurityTokenResolver;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.util.WSSecurityProcessorUtils;
import com.l7tech.util.Config;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.SAXException;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class ServerWssDigest extends AbstractMessageTargetableServerAssertion<WssDigest> {
    @Inject
    Config config;

    @Inject
    SecurityTokenResolver securityTokenResolver;

    private final String[] varsUsed;

    //- PUBLIC

    public ServerWssDigest(@NotNull WssDigest assertion) {
        this(assertion, null);
    }

    public ServerWssDigest(@NotNull WssDigest assertion, @Nullable AuditFactory auditFactory) {
        super(assertion, auditFactory);
        varsUsed = assertion.getVariablesUsed();
    }

    @Override
    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        if (!SecurityHeaderAddressableSupport.isLocalRecipient(assertion)) {
            logAndAudit(AssertionMessages.REQUESTWSS_NOT_FOR_US);
            return AssertionStatus.NONE;
        }

        return super.checkRequest( context );
    }

    //- PROTECTED

    @Override
    protected AssertionStatus doCheckRequest( final PolicyEnforcementContext context,
                                              final Message message,
                                              final String messageDescription,
                                              final AuthenticationContext authContext ) throws IOException, PolicyAssertionException {
        try {
            if (!message.isSoap()) {
                logAndAudit(AssertionMessages.REQUEST_NOT_SOAP);
                return AssertionStatus.NOT_APPLICABLE;
            }

            ProcessorResult wssResults;
            if ( isRequest() && !config.getBooleanProperty( ServerConfigParams.PARAM_WSS_PROCESSOR_LAZY_REQUEST,true) ) {
                wssResults = message.getSecurityKnob().getProcessorResult();
            } else {
                wssResults = WSSecurityProcessorUtils.getWssResults(message, messageDescription, securityTokenResolver, getAudit());
            }
            if (wssResults == null) {
                logAndAudit(AssertionMessages.REQUESTWSS_NO_SECURITY);
                if ( isRequest() ) {
                    context.setAuthenticationMissing();
                    context.setRequestPolicyViolated();
                }
                return AssertionStatus.AUTH_REQUIRED;
            }

            Map<String, ?> vars = context.getVariableMap(varsUsed, getAudit());

            Collection<UsernameToken> utoks = getUsernameTokens(wssResults);
            for (UsernameToken utok : utoks) {
                AssertionStatus ret = checkToken(utok, vars);
                if (ret == AssertionStatus.NONE) {
                    logAndAudit(AssertionMessages.USERDETAIL_INFO, "WSS Digest token validated successfully");
                    return ret;
                }
            }

            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "No conforming WSS Digest token was found in request");
            if ( isRequest() ) {
                context.setRequestPolicyViolated();
            }
            return AssertionStatus.FALSIFIED;
        } catch (SAXException e) {
            logAndAudit(AssertionMessages.REQUEST_BAD_XML);
            return AssertionStatus.BAD_REQUEST;
        }
    }

    //- PRIVATE

    private AssertionStatus checkToken(UsernameToken utok, Map<String, ?> vars) {
        if (!utok.isDigest())
            return falsifyFine("Ignoring UsernameToken that does not contain a digest password");

        String username = utok.getUsername();
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

        final String requiredUsername = assertion.getRequiredUsername();
        if (requiredUsername != null) {
            String expectedUsername = ExpandVariables.process(requiredUsername, vars, getAudit());
            if (!expectedUsername.equals(username))
                return falsify("Ignoring UsernameToken that does not contain a matching username");
        }

        final String requiredPass = assertion.getRequiredPassword();
        if (requiredPass == null)
            throw new AssertionStatusException(AssertionStatus.SERVER_ERROR, "WssDigest assertion has no password configured");
        String expectedDigest = UsernameTokenImpl.createPasswordDigest(ExpandVariables.process(requiredPass, vars, getAudit()).toCharArray(), created, nonce);

        if (!expectedDigest.equals(digest))
            return falsify("UsernameToken digest value does not match the expected value");

        // We're good
        return AssertionStatus.NONE;
    }

    private AssertionStatus falsifyFine(String msg) {
        logAndAudit(AssertionMessages.USERDETAIL_FINE, msg);
        return AssertionStatus.FALSIFIED;
    }

    private AssertionStatus falsify(String msg) {
        logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, msg);
        return AssertionStatus.FALSIFIED;
    }

    private Collection<UsernameToken> getUsernameTokens(ProcessorResult pr) {
        List<UsernameToken> ret = new ArrayList<>();

        for (XmlSecurityToken token : pr.getXmlSecurityTokens()) {
            if (token instanceof UsernameToken) {
                UsernameToken utok = (UsernameToken) token;
                ret.add(utok);
            }
        }

        return ret;
    }
}
