package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.security.token.UsernameTokenImpl;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.xml.saml.SamlAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.xmlsec.ResponseWssSecurityToken;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.AuthenticationContext;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;

import java.security.cert.X509Certificate;
import java.util.logging.Logger;

/**
 * @author alex
 */
public class ServerResponseWssSecurityToken extends ServerResponseWssSignature<ResponseWssSecurityToken> {
    private static final Logger logger = Logger.getLogger(ServerResponseWssSecurityToken.class.getName());

    public ServerResponseWssSecurityToken(ResponseWssSecurityToken assertion, ApplicationContext spring) {
        super(assertion, assertion, assertion, spring, ServerResponseWssSecurityToken.logger);
    }

    @Override
    protected int addDecorationRequirements( final PolicyEnforcementContext context,
                                             final AuthenticationContext authContext,
                                             final Document soapmsg,
                                             final DecorationRequirements wssReq) throws PolicyAssertionException {
        if (assertion.getTokenType() == SecurityTokenType.WSS_USERNAME) {
            // For backwards compatibility we are taking credentials from the default context
            // we may want to make this configurable at some point
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
            auditor.logAndAudit(AssertionMessages.RESPONSE_WSS_TOKEN_UNSUPPORTED_TYPE, assertion.getTokenType().getName());
            return -1;
        }
    }
}
