package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.security.token.SecurityTokenType;
import com.l7tech.common.security.token.UsernameTokenImpl;
import com.l7tech.common.security.xml.decorator.DecorationRequirements;
import com.l7tech.common.xml.saml.SamlAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.xmlsec.ResponseWssSecurityToken;
import com.l7tech.server.message.PolicyEnforcementContext;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;

import java.security.cert.X509Certificate;
import java.util.logging.Logger;

/**
 * @author alex
 */
public class ServerResponseWssSecurityToken extends ServerResponseWssSignature {
    private static final Logger logger = Logger.getLogger(ServerResponseWssSecurityToken.class.getName());
    private final ResponseWssSecurityToken assertion;

    public ServerResponseWssSecurityToken(ResponseWssSecurityToken assertion, ApplicationContext spring) {
        super(assertion, spring, ServerResponseWssSecurityToken.logger);
        this.assertion = assertion;
    }

    protected int addDecorationRequirements(PolicyEnforcementContext context, Document soapmsg, DecorationRequirements wssReq) throws PolicyAssertionException {
        if (assertion.getTokenType() == SecurityTokenType.WSS_USERNAME) {
            LoginCredentials creds = context.getCredentials();
            String name = creds.getLogin();
            char[] pass = null;
            if (creds.getFormat() == CredentialFormat.CLEARTEXT) {
                pass = creds.getCredentials();
            } else {
                Object payload = creds.getPayload();
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
                auditor.logAndAudit(AssertionMessages.RESPONSE_WSS_TOKEN_NO_USERNAME);
                return -1;
            }

            if (assertion.isIncludePassword()) {
                if (pass == null) {
                    auditor.logAndAudit(AssertionMessages.RESPONSE_WSS_TOKEN_NO_PASSWORD);
                    return -1;
                }
            } else {
                pass = null;
            }
            wssReq.setUsernameTokenCredentials(new UsernameTokenImpl(name, pass));
            wssReq.setSignUsernameToken(true);
            return 1;
        } else {
            auditor.logAndAudit(AssertionMessages.RESPONSE_WSS_TOKEN_UNSUPPORTED_TYPE, new String[] { assertion.getTokenType().getName()});
            return -1;
        }
    }
}
