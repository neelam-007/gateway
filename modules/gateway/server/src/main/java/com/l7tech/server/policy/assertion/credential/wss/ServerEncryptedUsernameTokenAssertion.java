/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.policy.assertion.credential.wss;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.server.audit.Auditor;
import com.l7tech.security.token.EncryptedKey;
import com.l7tech.security.token.SigningSecurityToken;
import com.l7tech.security.token.UsernameToken;
import com.l7tech.security.token.XmlSecurityToken;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.security.xml.processor.ProcessorResultUtil;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.util.CausedIOException;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.message.SecurityKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.wss.EncryptedUsernameTokenAssertion;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.logging.Logger;
import java.security.GeneralSecurityException;

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
        this.auditor = new Auditor(this, springContext, logger);
    }

    @Override
    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException
    {
        if (!data.getRecipientContext().localRecipient()) {
            auditor.logAndAudit(AssertionMessages.WSS_BASIC_FOR_ANOTHER_RECIPIENT);
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
                auditor.logAndAudit(AssertionMessages.WSS_BASIC_NOT_SOAP, messageDescription);
                return AssertionStatus.NOT_APPLICABLE;
            }
            wssResults = message.getSecurityKnob().getProcessorResult();
        } catch (SAXException e) {
            throw new CausedIOException("Request declared as XML but is not well-formed", e);
        }
        if (wssResults == null) {
            auditor.logAndAudit(AssertionMessages.WSS_BASIC_NO_CREDENTIALS, messageDescription);
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
                        logger.fine("Ignoring UsernameToken signging token that was not an EncryptedKey");
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

                if ( isRequest() ) {
                    // Configure the eventual response to reuse this EncryptedKey
                    try {
                        // Since it's a signing token it must already have been unwrapped
                        final String encryptedKeySha1 = signingToken.getEncryptedKeySHA1();
                        addDeferredAssertion(context, encryptedKeySha1, signingToken.getSecretKey());
                    } catch (InvalidDocumentFormatException e) {
                        throw new IllegalStateException(e); // can't happen -- it's a signing token
                    } catch (GeneralSecurityException e) {
                        throw new IllegalStateException(e); // can't happen -- it's a signing token
                    }
                }
                
                return AssertionStatus.NONE;
            }
        }
        auditor.logAndAudit(AssertionMessages.WSS_BASIC_CANNOT_FIND_ENC_CREDENTIALS, messageDescription);
        // we get here because there were no credentials found in the format we want
        // therefore this assertion was violated
        if ( isRequest() ) {
            context.setRequestPolicyViolated();
        }
        return AssertionStatus.AUTH_REQUIRED;
    }

    @Override
    protected Auditor getAuditor() {
        return auditor;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(ServerEncryptedUsernameTokenAssertion.class.getName());

    private final EncryptedUsernameTokenAssertion data;
    private final Auditor auditor;

    private void addDeferredAssertion(final PolicyEnforcementContext policyEnforcementContext,
                                      final String encryptedKeySha1,
                                      final byte[] secretKey) {
        policyEnforcementContext.addDeferredAssertion(this, new AbstractServerAssertion<EncryptedUsernameTokenAssertion>(data) {
            @Override
            public AssertionStatus checkRequest(final PolicyEnforcementContext context)
                    throws IOException, PolicyAssertionException
            {
                try {
                    if (!context.getResponse().isSoap()) {
                        auditor.logAndAudit(AssertionMessages.WSS_BASIC_UNABLE_TO_ATTACH_TOKEN);
                        return AssertionStatus.NOT_APPLICABLE;
                    }
                } catch (SAXException e) {
                    auditor.logAndAudit(AssertionMessages.WSS_BASIC_UNABLE_TO_ATTACH_TOKEN);
                    throw new CausedIOException(e);
                }

                SecurityKnob sk = context.getResponse().getSecurityKnob();
                DecorationRequirements respReq = sk.getAlternateDecorationRequirements(data.getRecipientContext());
                respReq.setEncryptedKeySha1(encryptedKeySha1);
                respReq.setEncryptedKey(secretKey);

                return AssertionStatus.NONE;
            }
        });
    }

}
