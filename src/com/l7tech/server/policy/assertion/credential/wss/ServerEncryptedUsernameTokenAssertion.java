/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion.credential.wss;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.security.token.EncryptedKey;
import com.l7tech.common.security.token.SigningSecurityToken;
import com.l7tech.common.security.token.UsernameToken;
import com.l7tech.common.security.token.XmlSecurityToken;
import com.l7tech.common.security.xml.processor.ProcessorResult;
import com.l7tech.common.security.xml.processor.ProcessorResultUtil;
import com.l7tech.common.security.xml.decorator.DecorationRequirements;
import com.l7tech.common.util.CausedIOException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.wss.EncryptedUsernameTokenAssertion;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Ensures that a UsernameToken was present in the request, was encrypted, and was signed with the same token that
 * signed the timestamp.
 */
public class ServerEncryptedUsernameTokenAssertion implements ServerAssertion {
    final private EncryptedUsernameTokenAssertion data;
    private final Auditor auditor;

    public ServerEncryptedUsernameTokenAssertion(EncryptedUsernameTokenAssertion data, ApplicationContext springContext) {
        this.data = data;
        this.auditor = new Auditor(this, springContext, logger);
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException
    {
        if (!data.getRecipientContext().localRecipient()) {
            auditor.logAndAudit(AssertionMessages.WSS_BASIC_FOR_ANOTHER_RECIPIENT);
            return AssertionStatus.NONE;
        }
        ProcessorResult wssResults;
        try {
            if (!context.getRequest().isSoap()) {
                auditor.logAndAudit(AssertionMessages.WSS_BASIC_NOT_SOAP);
                return AssertionStatus.NOT_APPLICABLE;
            }
            wssResults = context.getRequest().getSecurityKnob().getProcessorResult();
        } catch (SAXException e) {
            throw new CausedIOException("Request declared as XML but is not well-formed", e);
        }
        if (wssResults == null) {
            auditor.logAndAudit(AssertionMessages.WSS_BASIC_NO_CREDENTIALS);
            context.setAuthenticationMissing();
            context.setRequestPolicyViolated();
            return AssertionStatus.AUTH_REQUIRED;
        }

        XmlSecurityToken[] tokens = wssResults.getXmlSecurityTokens();
        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i] instanceof UsernameToken) {
                UsernameToken utok = (UsernameToken)tokens[i];

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
                for (int j = 0; j < signingTokens.length; j++) {
                    SigningSecurityToken stok = signingTokens[j];
                    if (!(stok instanceof EncryptedKey)) {
                        logger.fine("Ignoring UsernameToken signging token that was not an EncryptedKey");
                        continue;
                    }
                    signingToken = (EncryptedKey)stok;
                }

                if (signingToken == null) {
                    logger.fine("Ignoring UsernameToken that was not signed by an EncryptedKey");
                    continue;
                }

                // We're happy with this username token.  Proceed.
                String user = utok.getUsername();
                char[] pass = utok.getPassword();
                if (pass == null) pass = new char[0];
                LoginCredentials creds = LoginCredentials.makePasswordCredentials(user, pass, WssBasic.class);
                context.setCredentials(creds);

                // Configure the eventual response to reuse this EncryptedKey
                String encryptedKeySha1 = signingToken.getEncryptedKeySHA1();
                DecorationRequirements respReq = context.getResponse().getSecurityKnob().getAlternateDecorationRequirements(data.getRecipientContext());
                respReq.setEncryptedKeySha1(encryptedKeySha1);
                respReq.setEncryptedKey(signingToken.getSecretKey());
                return AssertionStatus.NONE;
            }
        }
        auditor.logAndAudit(AssertionMessages.WSS_BASIC_CANNOT_FIND_ENC_CREDENTIALS);
        // we get here because there were no credentials found in the format we want
        // therefore this assertion was violated
        context.setRequestPolicyViolated();
        return AssertionStatus.AUTH_REQUIRED;
    }

    private final Logger logger = Logger.getLogger(getClass().getName());
}
