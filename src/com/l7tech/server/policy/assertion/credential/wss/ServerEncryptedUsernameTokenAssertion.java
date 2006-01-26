/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion.credential.wss;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.security.token.*;
import com.l7tech.common.security.xml.processor.ProcessorResult;
import com.l7tech.common.util.CausedIOException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.wss.EncryptedUsernameTokenAssertion;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;
import org.w3c.dom.Element;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class ServerEncryptedUsernameTokenAssertion implements ServerAssertion {
    final private EncryptedUsernameTokenAssertion data;
    private final Auditor auditor;

    public ServerEncryptedUsernameTokenAssertion(EncryptedUsernameTokenAssertion data, ApplicationContext springContext) {
        this.data = data;
        this.auditor = new Auditor(this, springContext, logger);
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
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
                if (isTokenEncrypted(wssResults, tokens[i])) {
                    SigningSecurityToken sigtok = getSigningSecurityToken(wssResults, tokens[i]);
                    if (sigtok != null) {
                        if (sigtok instanceof EncryptedKey) {
                            EncryptedKey encryptedKey = (EncryptedKey)sigtok;
                            String encryptedKeySha1 = encryptedKey.getEncryptedKeySHA1();
                            context.getResponse().getSecurityKnob().getAlternateDecorationRequirements(data.getRecipientContext());
                            return AssertionStatus.NONE;
                        }
                    }
                }
            }
        }
        auditor.logAndAudit(AssertionMessages.WSS_BASIC_CANNOT_FIND_ENC_CREDENTIALS);
        // we get here because there were no credentials found in the format we want
        // therefore this assertion was violated
        context.setRequestPolicyViolated();
        return AssertionStatus.AUTH_REQUIRED;
    }

    private boolean isTokenEncrypted(ProcessorResult wssResults, XmlSecurityToken token) {
        EncryptedElement[] enc = wssResults.getElementsThatWereEncrypted();
        Element tokel = token.asElement();
        for (int i = 0; i < enc.length; i++) {
            EncryptedElement encryptedElement = enc[i];
            if (encryptedElement.asElement() == tokel)
                return true;
        }
        return false;
    }

    /** @return the signging security token for this token, or null if the token was not signed. */
    private SigningSecurityToken getSigningSecurityToken(ProcessorResult wssResults, XmlSecurityToken token) {
        SignedElement[] sig = wssResults.getElementsThatWereSigned();
        Element tokel = token.asElement();
        for (int i = 0; i < sig.length; i++) {
            SignedElement signedElement = sig[i];
            if (signedElement.asElement() == tokel)
                return signedElement.getSigningSecurityToken();
        }
        return null;
    }


    private final Logger logger = Logger.getLogger(getClass().getName());
}
