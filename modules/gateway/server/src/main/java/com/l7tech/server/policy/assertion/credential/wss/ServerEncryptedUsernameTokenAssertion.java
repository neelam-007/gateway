package com.l7tech.server.policy.assertion.credential.wss;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.server.ServerConfig;
import com.l7tech.security.token.EncryptedKey;
import com.l7tech.security.token.SigningSecurityToken;
import com.l7tech.security.token.UsernameToken;
import com.l7tech.security.token.XmlSecurityToken;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.security.xml.processor.ProcessorResultUtil;
import com.l7tech.security.xml.SecurityTokenResolver;
import com.l7tech.util.CausedIOException;
import com.l7tech.message.Message;
import com.l7tech.message.MessageRole;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.wss.EncryptedUsernameTokenAssertion;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.server.util.WSSecurityProcessorUtils;
import com.l7tech.util.Config;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import java.io.IOException;

/**
 * Ensures that a UsernameToken was present in the request, was encrypted, and was signed with the same token that
 * signed the timestamp.
 */
public class ServerEncryptedUsernameTokenAssertion extends AbstractMessageTargetableServerAssertion<EncryptedUsernameTokenAssertion> {

    //- PUBLIC

    public ServerEncryptedUsernameTokenAssertion( final EncryptedUsernameTokenAssertion data,
                                                  final ApplicationContext springContext ) {
        super(data,data);
        this.data = data;
        this.config = springContext.getBean("serverConfig", Config.class);
        this.securityTokenResolver = springContext.getBean("securityTokenResolver", SecurityTokenResolver.class);
    }

    @Override
    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException
    {
        if (!data.getRecipientContext().localRecipient()) {
            logAndAudit(AssertionMessages.WSS_BASIC_FOR_ANOTHER_RECIPIENT);
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
        ProcessorResult wssResults;
        try {
            if (!message.isSoap()) {
                logAndAudit(AssertionMessages.WSS_BASIC_NOT_SOAP, messageDescription);
                return AssertionStatus.NOT_APPLICABLE;
            }
            if ( isRequest() && !config.getBooleanProperty(ServerConfig.PARAM_WSS_PROCESSOR_LAZY_REQUEST,true) ) {
                wssResults = message.getSecurityKnob().getProcessorResult();
                message.getSecurityKnob().setNeedsSignatureConfirmations(true);
            } else {
                wssResults = WSSecurityProcessorUtils.getWssResults(message, messageDescription, securityTokenResolver, getAudit());
                if (message.getRelated(MessageRole.REQUEST) != null) {
                    message.getRelated(MessageRole.REQUEST).getSecurityKnob().setNeedsSignatureConfirmations(true);
                }
            }
        } catch (SAXException e) {
            throw new CausedIOException("Request declared as XML but is not well-formed", e);
        }
        if (wssResults == null) {
            logAndAudit(AssertionMessages.WSS_BASIC_NO_CREDENTIALS, messageDescription);
            if ( isRequest() ) {
                context.setAuthenticationMissing();
                context.setRequestPolicyViolated();
            }
            return AssertionStatus.AUTH_REQUIRED;
        }

        XmlSecurityToken[] tokens = wssResults.getXmlSecurityTokens();
        for (XmlSecurityToken token : tokens) {
            if (token instanceof UsernameToken) {
                UsernameToken utok = (UsernameToken) token;

                if (!ProcessorResultUtil.nodeIsPresent(utok.asElement(), wssResults.getElementsThatWereEncrypted())) {
                    logger.fine("Ignoring UsernameToken that was not encrypted");
                    continue;
                }

                SigningSecurityToken[] signingTokens = wssResults.getSigningTokens(utok.asElement());
                if (signingTokens == null || signingTokens.length < 1) {
                    logger.fine("Ignoring UsernameToken that was not signed");
                    continue;
                }

                EncryptedKey signingToken = null;
                for (SigningSecurityToken stok : signingTokens) {
                    if (!(stok instanceof EncryptedKey)) {
                        logger.fine("Ignoring UsernameToken signing token that was not an EncryptedKey");
                        continue;
                    }
                    signingToken = (EncryptedKey) stok;
                }

                if (signingToken == null) {
                    logger.fine("Ignoring UsernameToken that was not signed by an EncryptedKey");
                    continue;
                }

                // We're happy with this username token.  Proceed.
                LoginCredentials creds = LoginCredentials.makeLoginCredentials(utok, WssBasic.class, signingToken);
                authContext.addCredentials(creds);

                return AssertionStatus.NONE;
            }
        }
        logAndAudit(AssertionMessages.WSS_BASIC_CANNOT_FIND_ENC_CREDENTIALS, messageDescription);
        // we get here because there were no credentials found in the format we want
        // therefore this assertion was violated
        if ( isRequest() ) {
            context.setRequestPolicyViolated();
        }
        return AssertionStatus.AUTH_REQUIRED;
    }

    //- PRIVATE

    private final EncryptedUsernameTokenAssertion data;
    private final Config config;
    private final SecurityTokenResolver securityTokenResolver;

}
