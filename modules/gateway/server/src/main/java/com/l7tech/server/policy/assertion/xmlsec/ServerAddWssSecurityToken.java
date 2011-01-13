package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.xmlsec.AddWssSecurityToken;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.security.token.UsernameTokenImpl;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.xml.saml.SamlAssertion;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.w3c.dom.Document;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;

/**
 * @author alex
 */
public class ServerAddWssSecurityToken extends AbstractMessageTargetableServerAssertion<AddWssSecurityToken> {
    private static final Logger logger = Logger.getLogger(ServerAddWssSecurityToken.class.getName());

    private final Auditor auditor;
    private final String[] variableNames;
    private final AddWssSignatureSupport addWssSignatureSupport;
    private final AddWssEncryptionSupport addWssEncryptionSupport;

    public ServerAddWssSecurityToken( final AddWssSecurityToken assertion, final BeanFactory beanFactory, final ApplicationEventPublisher eventPub ) {
        super(assertion, assertion);
        this.auditor = new Auditor(this, beanFactory, eventPub, logger);
        this.variableNames = assertion.getVariablesUsed();
        this.addWssEncryptionSupport = new AddWssEncryptionSupport(auditor, logger, assertion, assertion, assertion);
        this.addWssSignatureSupport = new AddWssSignatureSupport(auditor, assertion, beanFactory, shouldFailIfNoElementsToSign(assertion), Assertion.isResponse(assertion));
    }

    @Override
    protected AssertionStatus doCheckRequest(PolicyEnforcementContext context, Message message, String messageDescription, AuthenticationContext authContext)
            throws IOException, PolicyAssertionException
    {
        DecorationRequirements dreq = message.getSecurityKnob().getAlternateDecorationRequirements(assertion.getRecipientContext());
        dreq.setProtectTokens(assertion.isProtectTokens());

        SecurityTokenType tokenType = assertion.getTokenType();
        if (SecurityTokenType.WSS_USERNAME == tokenType) {
            return addUsernameToken(context, message, messageDescription, authContext, dreq);
        } else if (SecurityTokenType.WSSC_CONTEXT == tokenType) {
            return addWsscContext(context, message, messageDescription, authContext, dreq);
        } else if (SecurityTokenType.SAML_ASSERTION == tokenType) {
            return addSamlAssertion(context, message, messageDescription, authContext, dreq);
        } else if (SecurityTokenType.WSS_ENCRYPTEDKEY == tokenType) {
            return addEncryptedKey(context, message, messageDescription, authContext, dreq);
        } else {
            auditor.logAndAudit(AssertionMessages.ADD_WSS_TOKEN_UNSUPPORTED_TYPE, assertion.getTokenType().getName());
            return AssertionStatus.SERVER_ERROR;
        }
    }

    private static boolean shouldFailIfNoElementsToSign(AddWssSecurityToken assertion) {
        return SecurityTokenType.WSS_USERNAME == assertion.getTokenType();
    }

    private AssertionStatus addEncryptedKey(PolicyEnforcementContext context, Message message, String messageDescription, AuthenticationContext authContext, DecorationRequirements dreq) throws IOException, PolicyAssertionException {
        AssertionStatus ret = addWssSignatureSupport.applySignatureDecorationRequirements(context, message, messageDescription, authContext, true, new AddWssSignatureSupport.SignedElementSelector() {
            @Override
            public int selectElementsToSign(PolicyEnforcementContext context, AuthenticationContext authContext, Document soapmsg, DecorationRequirements wssReq, Message targetMessage) throws PolicyAssertionException {
                return 1;
            }
        });
        dreq.setSenderMessageSigningCertificate(null);
        dreq.setSenderSamlToken(null);
        dreq.setSenderMessageSigningPrivateKey(null);
        dreq.setIncludeTimestamp(true);
        dreq.setSignTimestamp(true);
        return ret;
    }

    private AssertionStatus addSamlAssertion(PolicyEnforcementContext context, Message message, String messageDescription, AuthenticationContext authContext, DecorationRequirements dreq) {
        throw new UnsupportedOperationException("Not yet implemented - addSamlAssertion");
    }

    private AssertionStatus addWsscContext(PolicyEnforcementContext context, Message message, String messageDescription, AuthenticationContext authContext, DecorationRequirements dreq) {
        throw new UnsupportedOperationException("Not yet implemented - addWsscContext");
    }

    private AssertionStatus addUsernameToken(PolicyEnforcementContext context, Message message, String messageDescription, AuthenticationContext authContext, DecorationRequirements dreq) throws IOException, PolicyAssertionException {
        if (assertion.isUseLastGatheredCredentials()) {
            return applyUsernameTokenGatheredCredentialsDecorationRequirements(context, message, messageDescription, authContext);
        }

        dreq.setSignUsernameToken(assertion.isProtectTokens());
        return ServerAddWssUsernameToken.applyUsernameTokenSpecifiedCredentialsDecorationRequirements(addWssEncryptionSupport, context, message, messageDescription, assertion.getRecipientContext(),
                assertion.getUsername(), assertion.isIncludePassword() ? assertion.getPassword() : null, variableNames,
                assertion.isIncludeCreated(), assertion.isEncrypt(), assertion.isIncludeNonce(), assertion.isDigest(), this);
    }

    private AssertionStatus applyUsernameTokenGatheredCredentialsDecorationRequirements(PolicyEnforcementContext context, Message message, String messageDescription, AuthenticationContext authContext)
            throws PolicyAssertionException, IOException {
        AddWssSignatureSupport.SignedElementSelector signedElementSelector = new AddWssSignatureSupport.SignedElementSelector() {
            @Override
            public int selectElementsToSign(PolicyEnforcementContext context, AuthenticationContext authContext, Document soapmsg, DecorationRequirements wssReq, Message targetMessage)
                    throws PolicyAssertionException
            {
                LoginCredentials creds = context.getDefaultAuthenticationContext().getLastCredentials();
                String name = creds == null ? null : creds.getLogin();
                char[] pass = null;
                if (creds != null && creds.getFormat() == CredentialFormat.CLEARTEXT) {
                    pass = creds.getCredentials();
                } else {
                    Object payload = creds == null ? null : creds.getPayload();
                    if (payload instanceof X509Certificate) {
                        X509Certificate x509Certificate = (X509Certificate) payload;
                        name = x509Certificate.getSubjectDN().getName();
                    } else if (payload instanceof SamlAssertion) {
                        SamlAssertion samlAssertion = (SamlAssertion) payload;
                        name = samlAssertion.getNameIdentifierValue();
                        if (name == null) {
                            X509Certificate cert = samlAssertion.getSubjectCertificate();
                            if (cert != null) name = cert.getSubjectDN().getName();
                        }
                    }
                }

                if (name == null) {
                    auditor.logAndAudit(AssertionMessages.ADD_WSS_TOKEN_NO_USERNAME);
                    return -1;
                }

                if (assertion.isIncludePassword()) {
                    if (pass == null) {
                        auditor.logAndAudit(AssertionMessages.ADD_WSS_TOKEN_NO_PASSWORD);
                        return -1;
                    }
                } else {
                    pass = null;
                }
                wssReq.setUsernameTokenCredentials(new UsernameTokenImpl(name, pass));
                if (assertion.isProtectTokens()) {
                    wssReq.setSignUsernameToken(true);
                    wssReq.setSignTimestamp(true);
                }
                return 1;
            }
        };
        return addWssSignatureSupport.applySignatureDecorationRequirements(context, message, messageDescription, authContext, true, signedElementSelector);
    }

    @Override
    protected Audit getAuditor() {
        return auditor;
    }
}
